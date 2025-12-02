import { useState, useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useGetBenchmarkRun } from '@/api/generated/benchmarks-controller/benchmarks-controller'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ArrowLeft, Loader2, CheckCircle, XCircle, Clock, PlayCircle, Activity, ChevronDown, ChevronUp, Info } from 'lucide-react'
import { format } from 'date-fns'

const getStateColor = (state: string) => {
  switch (state) {
    case 'COMPLETED':
      return 'default'
    case 'FAILED':
      return 'destructive'
    case 'PENDING':
      return 'secondary'
    case 'RUNNING':
      return 'default'
    default:
      return 'outline'
  }
}

const getStateClassName = (state: string) => {
  return state === 'COMPLETED' ? 'bg-green-600 hover:bg-green-700' : ''
}

const getStateIcon = (state: string) => {
  switch (state) {
    case 'COMPLETED':
      return <CheckCircle className="h-4 w-4" />
    case 'FAILED':
      return <XCircle className="h-4 w-4" />
    case 'PENDING':
      return <Clock className="h-4 w-4" />
    case 'RUNNING':
      return <Activity className="h-4 w-4 animate-pulse" />
    default:
      return <PlayCircle className="h-4 w-4" />
  }
}

export function BenchmarkRunDetailPage() {
  const { benchmarkId, runId } = useParams<{ benchmarkId: string; runId: string }>()
  const { data, isLoading, error } = useGetBenchmarkRun(benchmarkId!, runId!)
  const [expandedK6Outputs, setExpandedK6Outputs] = useState<Set<string>>(new Set())
  const [expandedOperationMetrics, setExpandedOperationMetrics] = useState<Set<string>>(new Set())
  const [selectedLoad, setSelectedLoad] = useState<string>('')

  // Set initial selected load when data loads
  useEffect(() => {
    if (data?.targetTestCases && data.targetTestCases.length > 0 && !selectedLoad) {
      const sortedCases = [...data.targetTestCases].sort((a, b) => a.load - b.load)
      setSelectedLoad(sortedCases[0].load.toString())
    }
  }, [data, selectedLoad])

  const toggleK6Output = (id: string) => {
    setExpandedK6Outputs((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  const toggleOperationMetrics = (id: string) => {
    setExpandedOperationMetrics((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error || !data) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-destructive">
          Error loading benchmark run: {(error as Error)?.message || 'Unknown error'}
        </p>
      </div>
    )
  }

  // Calculate key metrics for dashboard
  const totalTestCases = data.targetTestCases?.length || 0
  const completedTestCases = data.targetTestCases?.filter(tc => tc.status === 'COMPLETED').length || 0
  const failedTestCases = data.targetTestCases?.filter(tc => tc.status === 'FAILED').length || 0

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-4">
          <Link to={`/benchmarks/${benchmarkId}/runs`}>
            <Button variant="outline" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold">Benchmark Run Dashboard</h1>
              <Badge variant={getStateColor(data.state)} className={`flex items-center gap-1 ${getStateClassName(data.state)}`}>
                {getStateIcon(data.state)}
                {data.state}
              </Badge>
            </div>
            <p className="text-muted-foreground mt-1 font-mono text-sm">{data.id}</p>
          </div>
        </div>
      </div>

      {/* Dashboard Stats Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm font-medium text-muted-foreground">Number of Loads</p>
              <p className="text-3xl font-bold mt-2">{totalTestCases}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm font-medium text-muted-foreground">Completed</p>
              <p className="text-3xl font-bold mt-2 text-green-600">{completedTestCases}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm font-medium text-muted-foreground">Failed</p>
              <p className="text-3xl font-bold mt-2 text-red-600">{failedTestCases}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm font-medium text-muted-foreground">Operations</p>
              <p className="text-3xl font-bold mt-2">
                {data.experimentResults?.operationExperimentResults
                  ? Object.keys(data.experimentResults.operationExperimentResults).length
                  : 0}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Total Domain Metric - Hero Section */}
      {data.experimentResults && (
        <Card className="border-2 border-green-200 dark:border-green-800">
          <CardContent className="pt-6">
            <div className="p-6 rounded-lg bg-gradient-to-r from-green-50 to-emerald-50 dark:from-green-950/30 dark:to-emerald-950/30">
              <div className="flex flex-col md:flex-row items-center justify-between gap-4">
                <div className="text-center md:text-left">
                  <p className="text-lg font-bold text-green-900 dark:text-green-100 mb-2">Total Domain Metric (TDM)</p>
                  <p className="text-sm text-muted-foreground">Overall scalability of the System Under Test</p>
                </div>
                <div className="text-center">
                  <p className="text-5xl font-bold text-green-700 dark:text-green-400">
                    {data.experimentResults.totalDomainMetric.toFixed(4)}
                  </p>
                  <p className="text-sm text-muted-foreground mt-2 font-medium">
                    {data.experimentResults.totalDomainMetric >= 0.95 ? 'Excellent scalability' :
                     data.experimentResults.totalDomainMetric >= 0.8 ? 'Good scalability' :
                     data.experimentResults.totalDomainMetric >= 0.6 ? 'Moderate scalability' :
                     'Poor scalability'}
                  </p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Performance Charts Section - Placeholder for graphs */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Performance Over Load</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64 flex items-center justify-center border-2 border-dashed border-muted rounded-lg bg-muted/20">
              <p className="text-sm text-muted-foreground">Chart: Response time vs Load</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Scalability Metrics</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64 flex items-center justify-center border-2 border-dashed border-muted rounded-lg bg-muted/20">
              <p className="text-sm text-muted-foreground">Chart: Operation scalability comparison</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Run Metadata */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Run Information</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-sm">
            <div>
              <p className="text-muted-foreground mb-1">Created</p>
              <p className="font-medium">{format(new Date(data.createdAt), 'PPp')}</p>
            </div>
            <div>
              <p className="text-muted-foreground mb-1">Last Updated</p>
              <p className="font-medium">{format(new Date(data.updatedAt), 'PPp')}</p>
            </div>
            <div>
              <p className="text-muted-foreground mb-1">Status</p>
              <Badge variant={getStateColor(data.state)} className={`flex items-center gap-1 w-fit ${getStateClassName(data.state)}`}>
                {getStateIcon(data.state)}
                {data.state}
              </Badge>
            </div>
            <div>
              <p className="text-muted-foreground mb-1">Benchmark ID</p>
              <p className="font-mono text-xs">{data.benchmarkId}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Operation Experiment Results */}
      {data.experimentResults && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Operation Results Summary</CardTitle>
          </CardHeader>
          <CardContent>

            {data.experimentResults.operationExperimentResults && Object.keys(data.experimentResults.operationExperimentResults).length > 0 && (() => {
              // Calculate max load from target test cases
              const maxLoad = data.targetTestCases && data.targetTestCases.length > 0
                ? Math.max(...data.targetTestCases.map(tc => tc.load))
                : null

              return (
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="py-2">Operation ID</TableHead>
                        <TableHead className="text-right py-2">Total Requests</TableHead>
                        <TableHead className="text-right py-2">Failed Requests</TableHead>
                        <TableHead className="text-right py-2">Scalability Footprint</TableHead>
                        <TableHead className="text-right py-2">Scalability Gap</TableHead>
                        <TableHead className="text-right py-2">Performance Offset</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {Object.entries(data.experimentResults.operationExperimentResults).map(([operationId, result]: [string, any]) => {
                        // Determine if we should show N/A for scalability metrics
                        const scalabilityFootprint = result.scalabilityFootprint
                        const shouldShowNA = scalabilityFootprint === null || scalabilityFootprint === undefined ||
                                            (maxLoad !== null && scalabilityFootprint === maxLoad)

                        const tooltipMessage = scalabilityFootprint === null || scalabilityFootprint === undefined
                          ? "Scalability Gap/Performance Offset is not defined because the operation always fails"
                          : "Scalability Gap/Performance Offset is not defined because the operation never fails at any load"

                        return (
                          <TableRow key={operationId}>
                            <TableCell className="font-mono text-xs font-medium py-2">{result.operationId}</TableCell>
                            <TableCell className="text-right font-mono text-xs py-2">{result.totalRequests}</TableCell>
                            <TableCell className="text-right font-mono text-xs py-2">{result.failedRequests}</TableCell>
                            <TableCell className="text-right font-mono text-xs py-2">
                              {scalabilityFootprint !== undefined && scalabilityFootprint !== null ? (
                                Math.round(scalabilityFootprint)
                              ) : (
                                <TooltipProvider>
                                  <Tooltip>
                                    <TooltipTrigger asChild>
                                      <span className="inline-flex items-center gap-1 cursor-help">
                                        N/A <Info className="h-3 w-3 text-muted-foreground" />
                                      </span>
                                    </TooltipTrigger>
                                    <TooltipContent>
                                      <p className="max-w-xs">Scalability Footprint is not available because the operation always fails</p>
                                    </TooltipContent>
                                  </Tooltip>
                                </TooltipProvider>
                              )}
                            </TableCell>
                            <TableCell className="text-right font-mono text-xs py-2">
                              {!shouldShowNA && result.scalabilityGap !== undefined ? (
                                result.scalabilityGap.toFixed(2)
                              ) : (
                                <TooltipProvider>
                                  <Tooltip>
                                    <TooltipTrigger asChild>
                                      <span className="inline-flex items-center gap-1 cursor-help">
                                        N/A <Info className="h-3 w-3 text-muted-foreground" />
                                      </span>
                                    </TooltipTrigger>
                                    <TooltipContent>
                                      <p className="max-w-xs">{tooltipMessage}</p>
                                    </TooltipContent>
                                  </Tooltip>
                                </TooltipProvider>
                              )}
                            </TableCell>
                            <TableCell className="text-right font-mono text-xs py-2">
                              {!shouldShowNA && result.performanceOffset !== undefined ? (
                                result.performanceOffset.toFixed(2)
                              ) : (
                                <TooltipProvider>
                                  <Tooltip>
                                    <TooltipTrigger asChild>
                                      <span className="inline-flex items-center gap-1 cursor-help">
                                        N/A <Info className="h-3 w-3 text-muted-foreground" />
                                      </span>
                                    </TooltipTrigger>
                                    <TooltipContent>
                                      <p className="max-w-xs">{tooltipMessage}</p>
                                    </TooltipContent>
                                  </Tooltip>
                                </TooltipProvider>
                              )}
                            </TableCell>
                          </TableRow>
                        )
                      })}
                    </TableBody>
                  </Table>
                </div>
              )
            })()}
          </CardContent>
        </Card>
      )}

      {/* Test Cases Details - Collapsible Sections */}
      <div className="grid grid-cols-1 gap-6">
        {/* Baseline Test Case */}
        {data.baselineTestCase && (
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <CardTitle className="text-base">Baseline Test Case</CardTitle>
                  <Badge variant="default" className="bg-blue-600">Baseline</Badge>
                  <span className="text-sm font-medium text-muted-foreground">{data.baselineTestCase.load} users</span>
                  <Badge variant={getStateColor(data.baselineTestCase.status)} className={`flex items-center gap-1 ${getStateClassName(data.baselineTestCase.status)}`}>
                    {getStateIcon(data.baselineTestCase.status)}
                    {data.baselineTestCase.status}
                  </Badge>
                </div>
                <p className="text-xs font-mono text-muted-foreground">{data.baselineTestCase.baselineSutId}</p>
              </div>
              {data.baselineTestCase.startTimestamp && data.baselineTestCase.endTimestamp && (
                <p className="text-xs text-muted-foreground mt-2">
                  {format(new Date(data.baselineTestCase.startTimestamp), 'PPp')} - {format(new Date(data.baselineTestCase.endTimestamp), 'p')}
                </p>
              )}
            </CardHeader>
            <CardContent className="space-y-4">

              {/* Operation Metrics */}
              {data.baselineTestCase.operationMetrics && Object.keys(data.baselineTestCase.operationMetrics).length > 0 ? (
                <div className="space-y-3">
                  <p className="text-sm font-semibold">Operation Metrics</p>
                  <div className="overflow-x-auto">
                    <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="py-2">Operation ID</TableHead>
                        <TableHead className="text-right py-2">Total Requests</TableHead>
                        <TableHead className="text-right py-2">Failed</TableHead>
                        <TableHead className="text-right py-2">Mean Response</TableHead>
                        <TableHead className="text-right py-2">Std Dev</TableHead>
                        <TableHead className="text-right py-2">P95</TableHead>
                        <TableHead className="text-right py-2">P99</TableHead>
                        <TableHead className="text-right py-2">Scalability Threshold</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {Object.entries(data.baselineTestCase.operationMetrics).map(([operationId, metric]: [string, any]) => (
                        <TableRow key={operationId}>
                          <TableCell className="font-mono text-xs font-medium py-2">{metric.operationId}</TableCell>
                          <TableCell className="text-right font-mono text-xs py-2">{metric.totalRequests}</TableCell>
                          <TableCell className="text-right font-mono text-xs py-2">{metric.failedRequests}</TableCell>
                          <TableCell className="text-right font-mono text-xs py-2">
                            <span className="font-semibold text-amber-600 dark:text-amber-400">
                              {Number(metric.meanResponseTimeMs).toFixed(2)}ms
                            </span>
                          </TableCell>
                          <TableCell className="text-right font-mono text-xs py-2">{Number(metric.stdDevResponseTimeMs).toFixed(2)}ms</TableCell>
                          <TableCell className="text-right font-mono text-xs py-2">{Number(metric.p95DurationMs).toFixed(2)}ms</TableCell>
                          <TableCell className="text-right font-mono text-xs py-2">{Number(metric.p99DurationMs).toFixed(2)}ms</TableCell>
                          <TableCell className="text-right font-mono text-xs font-medium text-blue-600 dark:text-blue-400 py-2">
                            {Number(metric.scalabilityThreshold).toFixed(2)}ms
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                    </Table>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">No operation metrics available</p>
              )}

              {/* K6 Output */}
              {data.baselineTestCase.k6Output && (
                <div className="space-y-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => toggleK6Output('baseline')}
                    className="text-xs"
                  >
                    {expandedK6Outputs.has('baseline') ? (
                      <>
                        <ChevronUp className="h-3 w-3 mr-1" />
                        Hide k6 Output
                      </>
                    ) : (
                      <>
                        <ChevronDown className="h-3 w-3 mr-1" />
                        Show k6 Output
                      </>
                    )}
                  </Button>
                  {expandedK6Outputs.has('baseline') && (
                    <div className="p-3 rounded-lg bg-background border">
                      <pre className="text-xs font-mono whitespace-pre-wrap overflow-x-auto max-h-96 overflow-y-auto p-2 rounded bg-muted/30">
                        {data.baselineTestCase.k6Output}
                      </pre>
                    </div>
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {/* Target Test Cases Results */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">Target Test Case Results</CardTitle>
              {data.targetTestCases && data.targetTestCases.length > 0 && (
                <div className="flex items-center gap-2">
                  <span className="text-sm text-muted-foreground">Select Load:</span>
                  <Select value={selectedLoad} onValueChange={setSelectedLoad}>
                    <SelectTrigger className="w-[180px]">
                      <SelectValue placeholder="Select load" />
                    </SelectTrigger>
                    <SelectContent>
                      {[...data.targetTestCases]
                        .sort((a, b) => a.load - b.load)
                        .map((tc) => (
                          <SelectItem key={tc.id} value={tc.load.toString()}>
                            {tc.load} users
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                </div>
              )}
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {!data.targetTestCases || data.targetTestCases.length === 0 ? (
              <p className="text-center text-muted-foreground py-8">No test results available</p>
            ) : (
              (() => {
                const testCase = data.targetTestCases.find(tc => tc.load.toString() === selectedLoad)
                if (!testCase) return null

                const testCaseIndex = data.targetTestCases.findIndex(tc => tc.id === testCase.id)

                return (
                  <Card className="border-2">
                    <CardHeader className="pb-3">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <span className="text-base font-semibold">{testCase.load} users</span>
                          {testCase.loadFrequency !== undefined && (
                            <span className="text-sm text-muted-foreground">
                              ({(testCase.loadFrequency * 100).toFixed(0)}% probability)
                            </span>
                          )}
                          <Badge variant={getStateColor(testCase.status)} className={`text-xs ${getStateClassName(testCase.status)}`}>
                            {getStateIcon(testCase.status)}
                            {testCase.status}
                          </Badge>
                        </div>
                      </div>

                      {testCase.startTimestamp && testCase.endTimestamp && (
                        <p className="text-xs text-muted-foreground mt-2">
                          {format(new Date(testCase.startTimestamp), 'PPp')} - {format(new Date(testCase.endTimestamp), 'p')}
                        </p>
                      )}
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {/* Relative Domain Metric */}
                      {testCase.relativeDomainMetric !== undefined && (
                        <div className="p-4 rounded-lg bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-950/30 dark:to-indigo-950/30 border-2 border-blue-200 dark:border-blue-800">
                          <div className="flex flex-col md:flex-row items-center justify-between gap-3">
                            <div className="text-center md:text-left">
                              <p className="text-sm font-semibold text-blue-900 dark:text-blue-100 mb-1">Relative Domain Metric (RDM)</p>
                              <p className="text-xs text-muted-foreground">Probability of not failing under this load</p>
                            </div>
                            <div className="text-center">
                              <p className="text-3xl font-bold text-blue-700 dark:text-blue-400">
                                {testCase.relativeDomainMetric.toFixed(4)}
                              </p>
                              <p className="text-xs text-muted-foreground mt-1">
                                {testCase.relativeDomainMetric >= 0.95 * (testCase.loadFrequency || 1) ? 'Very high reliability' :
                                 testCase.relativeDomainMetric >= 0.8 * (testCase.loadFrequency || 1) ? 'High reliability' :
                                 testCase.relativeDomainMetric >= 0.5 * (testCase.loadFrequency || 1) ? 'Moderate reliability' :
                                 'High failure risk'}
                              </p>
                            </div>
                          </div>
                        </div>
                      )}

                      {/* Operation Metrics */}
                      {testCase.operationMetrics && Object.keys(testCase.operationMetrics).length > 0 ? (
                        <div className="space-y-3">
                          <p className="text-sm font-semibold">Operation Metrics</p>
                          <div className="overflow-x-auto">
                            <Table>
                        <TableHeader>
                          <TableRow>
                            <TableHead className="py-2">Operation ID</TableHead>
                            <TableHead className="text-center py-2">Status</TableHead>
                            <TableHead className="text-center py-2">Share</TableHead>
                            <TableHead className="text-right py-2">Total Requests</TableHead>
                            <TableHead className="text-right py-2">Failed</TableHead>
                            <TableHead className="text-right py-2">Mean Response</TableHead>
                            <TableHead className="text-right py-2">Std Dev</TableHead>
                            <TableHead className="text-right py-2">P95</TableHead>
                            <TableHead className="text-right py-2">P99</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {Object.entries(testCase.operationMetrics).map(([operationId, metric]: [string, any]) => (
                            <TableRow key={operationId}>
                              <TableCell className="font-mono text-xs font-medium py-2">{metric.operationId}</TableCell>
                              <TableCell className="text-center py-2">
                                {metric.passScalabilityThreshold !== undefined && (
                                  <Badge variant={metric.passScalabilityThreshold ? "default" : "destructive"} className={`text-xs ${metric.passScalabilityThreshold ? 'bg-green-600' : ''}`}>
                                    {metric.passScalabilityThreshold ? 'Pass' : 'Fail'}
                                  </Badge>
                                )}
                              </TableCell>
                              <TableCell className="text-center py-2">
                                {metric.scalabilityShare !== undefined && (
                                  <Badge variant="outline" className="text-xs font-mono">
                                    {metric.scalabilityShare.toFixed(4)}
                                  </Badge>
                                )}
                              </TableCell>
                              <TableCell className="text-right font-mono text-xs py-2">{metric.totalRequests}</TableCell>
                              <TableCell className="text-right font-mono text-xs py-2">{metric.failedRequests}</TableCell>
                              <TableCell className="text-right font-mono text-xs py-2">
                                <span className="font-semibold text-amber-600 dark:text-amber-400">
                                  {Number(metric.meanResponseTimeMs).toFixed(2)}ms
                                </span>
                              </TableCell>
                              <TableCell className="text-right font-mono text-xs py-2">{Number(metric.stdDevResponseTimeMs).toFixed(2)}ms</TableCell>
                              <TableCell className="text-right font-mono text-xs py-2">{Number(metric.p95DurationMs).toFixed(2)}ms</TableCell>
                              <TableCell className="text-right font-mono text-xs py-2">{Number(metric.p99DurationMs).toFixed(2)}ms</TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                            </Table>
                          </div>
                        </div>
                      ) : (
                        <p className="text-sm text-muted-foreground">No operation metrics available</p>
                      )}

                      {/* K6 Output */}
                      {testCase.k6Output && (
                        <div className="space-y-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => toggleK6Output(`target-${testCaseIndex}`)}
                            className="text-xs"
                          >
                            {expandedK6Outputs.has(`target-${testCaseIndex}`) ? (
                              <>
                                <ChevronUp className="h-3 w-3 mr-1" />
                                Hide k6 Output
                              </>
                            ) : (
                              <>
                                <ChevronDown className="h-3 w-3 mr-1" />
                                Show k6 Output
                              </>
                            )}
                          </Button>
                          {expandedK6Outputs.has(`target-${testCaseIndex}`) && (
                            <div className="p-3 rounded-lg bg-background border">
                              <pre className="text-xs font-mono whitespace-pre-wrap overflow-x-auto max-h-96 overflow-y-auto p-2 rounded bg-muted/30">
                                {testCase.k6Output}
                              </pre>
                            </div>
                          )}
                        </div>
                      )}
                    </CardContent>
                  </Card>
                )
              })()
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
