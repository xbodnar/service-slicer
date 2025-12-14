import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import type { TestCaseDto, TestSuiteDto } from '@/api/generated/openAPIDefinition.schemas'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ArrowLeft, Loader2, CheckCircle, XCircle, Clock, PlayCircle, Activity, RotateCcw } from 'lucide-react'
import { format } from 'date-fns'
import {useGetBenchmarkRun, useRestartBenchmarkRun} from "@/api/generated/benchmark-run-controller/benchmark-run-controller.ts";
import { useToast } from '@/components/ui/use-toast'
import { useQueryClient } from '@tanstack/react-query'
import {
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer,
    ComposedChart,
    RadarChart,
    PolarGrid,
    PolarAngleAxis,
    PolarRadiusAxis,
    Radar,
    Area
} from 'recharts'

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
  const { toast } = useToast()
  const queryClient = useQueryClient()
  const { data, isLoading, error, refetch } = useGetBenchmarkRun(runId!)

  // State for selected SUT and load
  const [selectedSutId, setSelectedSutId] = useState<string>('')
  const [selectedLoad, setSelectedLoad] = useState<string>('')
  const [showK6Output, setShowK6Output] = useState(false)

  // Initialize selections when data loads
  useEffect(() => {
    if (data?.testSuites && data.testSuites.length > 0 && !selectedSutId) {
      setSelectedSutId(data.testSuites[0].systemUnderTest.id)
      if (data.testSuites[0].testCases.length > 0) {
        const sortedTestCases = [...data.testSuites[0].testCases].sort((a, b) => a.load - b.load)
        setSelectedLoad(sortedTestCases[0].load.toString())
      }
    }
  }, [data, selectedSutId])

  // Reset K6 output visibility when selection changes
  useEffect(() => {
    setShowK6Output(false)
  }, [selectedSutId, selectedLoad])

  // Poll for updates when any test suite is PENDING or RUNNING
  useEffect(() => {
    if (!data) return

    const hasPendingOrRunning = data.testSuites?.some(
      (suite: TestSuiteDto) => suite.status === 'PENDING' || suite.status === 'RUNNING'
    )

    if (hasPendingOrRunning) {
      const pollInterval = setInterval(() => {
        refetch()
      }, 5000) // Poll every 5 seconds

      return () => clearInterval(pollInterval)
    }
  }, [data, refetch])

  const restartRunMutation = useRestartBenchmarkRun({
    mutation: {
      onSuccess: (updatedRun) => {
        toast({
          title: 'Run restarted',
          description: 'The benchmark run has been restarted successfully',
        })

        // Update the cache with the response from the restart API
        queryClient.setQueryData(
          [`/benchmark-runs/${runId}`],
          (oldData: any) => {
            if (!oldData) return oldData
            return {
              ...oldData,
              ...updatedRun
            }
          }
        )

        // Invalidate list query to refresh the list page
          queryClient.invalidateQueries({ queryKey: ['/benchmark-runs'], exact: true })
      },
      onError: (error: Error) => {
        toast({
          variant: 'destructive',
          title: 'Failed to restart run',
          description: error.message || 'An unknown error occurred',
        })
      },
    },
  })

  const handleRestartRun = () => {
    if (!runId) return
    restartRunMutation.mutate({ benchmarkRunId: runId })
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
  const numArchitectures = data.testSuites?.length || 0
  const numLoads = data.operationalProfile ? Object.keys(data.operationalProfile).length : 0
  const totalTestCases = data.testSuites?.reduce((sum, suite: TestSuiteDto) => sum + (suite.testCases?.length || 0), 0) || 0
  const completedTestCases = data.testSuites?.reduce((sum, suite: TestSuiteDto) =>
    sum + (suite.testCases?.filter((tc: TestCaseDto) => tc.status === 'COMPLETED').length || 0), 0) || 0
  const numOperations = new Set(
    data.usageProfile?.flatMap(bm => bm.steps.map(step => step.operationId)) || []
  ).size

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
              <Badge variant={getStateColor(data.status)} className={`flex items-center gap-1 ${getStateClassName(data.status)}`}>
                {getStateIcon(data.status)}
                {data.status}
              </Badge>
            </div>
            <p className="text-muted-foreground mt-1 font-mono text-sm">{data.id}</p>
          </div>
        </div>
        <Button
          variant="outline"
          onClick={handleRestartRun}
          disabled={data.status !== 'FAILED' || restartRunMutation.isPending}
        >
          {restartRunMutation.isPending ? (
            <>
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              Restarting...
            </>
          ) : (
            <>
              <RotateCcw className="h-4 w-4 mr-2" />
              Restart Run
            </>
          )}
        </Button>
      </div>

      {/* Dashboard Stats Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm font-medium text-muted-foreground">Architectures</p>
              <p className="text-3xl font-bold mt-2">{numArchitectures}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm font-medium text-muted-foreground">Load Levels</p>
              <p className="text-3xl font-bold mt-2">{numLoads}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm font-medium text-muted-foreground">Completed Test Cases</p>
              <p className="text-3xl font-bold mt-2">
                <span className={completedTestCases === totalTestCases ? 'text-green-600' : ''}>{completedTestCases}</span>
                <span className="text-muted-foreground text-xl"> / {totalTestCases}</span>
              </p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm font-medium text-muted-foreground">Operations</p>
              <p className="text-3xl font-bold mt-2">{numOperations}</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Run Information and Total Domain Metric - Side by Side */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Run Information */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Run Information</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div className="space-y-1">
                <p className="text-muted-foreground">Created</p>
                <p className="font-medium">{format(new Date(data.createdAt), 'PPp')}</p>
              </div>
              <div className="space-y-1">
                <p className="text-muted-foreground">Last Updated</p>
                <p className="font-medium">{format(new Date(data.updatedAt), 'PPp')}</p>
              </div>
              <div className="space-y-1">
                <p className="text-muted-foreground">Test Duration</p>
                <p className="font-medium">{data.testDuration || 'Default (1ms)'}</p>
              </div>
              <div className="space-y-1">
                <p className="text-muted-foreground">Status</p>
                <Badge variant={getStateColor(data.status)} className={`flex items-center gap-1 w-fit ${getStateClassName(data.status)}`}>
                  {getStateIcon(data.status)}
                  {data.status}
                </Badge>
              </div>
              <div className="space-y-1 sm:col-span-2">
                <p className="text-muted-foreground">Benchmark ID</p>
                <p className="font-mono text-xs break-all">{data.benchmarkId}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Total Domain Metric */}
        <Card className="border-2 border-green-200 dark:border-green-800">
          <CardHeader>
            <CardTitle className="text-base">Total Domain Metrics (TDM)</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {data.testSuites.map((suite: TestSuiteDto) => {
                const tdm = suite.testSuiteResults?.totalDomainMetric
                const hasResults = tdm !== undefined && tdm !== null
                const isFailed = suite.status === 'FAILED'

                return (
                  <div key={suite.id} className={`p-4 rounded-lg ${
                    hasResults ? 'bg-gradient-to-r from-green-50 to-emerald-50 dark:from-green-950/30 dark:to-emerald-950/30 border-2 border-green-200 dark:border-green-800' :
                    isFailed ? 'bg-destructive/10 border border-destructive/30' :
                    'bg-muted/20 border border-muted'
                  }`}>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium">{suite.systemUnderTest.name}</span>
                      </div>
                      <div className="text-right">
                        {hasResults ? (
                          <>
                            <p className="text-3xl font-bold text-green-700 dark:text-green-400">
                              {tdm.toFixed(4)}
                            </p>
                            <p className="text-xs text-muted-foreground mt-1">
                              {tdm >= 0.95 ? 'Excellent' :
                               tdm >= 0.8 ? 'Good' :
                               tdm >= 0.6 ? 'Moderate' :
                               'Poor'}
                            </p>
                          </>
                        ) : isFailed ? (
                          <div className="flex items-center gap-2 text-destructive">
                            <XCircle className="h-5 w-5" />
                            <span className="text-sm font-medium">Failed</span>
                          </div>
                        ) : suite.status === 'RUNNING' ? (
                          <div className="flex items-center gap-2 text-muted-foreground">
                            <Loader2 className="h-5 w-5 animate-spin" />
                            <span className="text-sm font-medium">Calculating...</span>
                          </div>
                        ) : suite.status === 'PENDING' ? (
                          <div className="flex items-center gap-2 text-muted-foreground">
                            <Clock className="h-5 w-5" />
                            <span className="text-sm font-medium">In Queue</span>
                          </div>
                        ) : (
                          <div className="flex items-center gap-2 text-muted-foreground">
                            <span className="text-sm font-medium">Unknown</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Charts Side by Side */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Domain Metric Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Domain Metric Across Load Levels</CardTitle>
          </CardHeader>
          <CardContent>
            {(() => {
              if (!data.operationalProfile) {
                return <p className="text-muted-foreground text-center py-8">No operational profile available</p>
              }

              // Get all loads from operational profile
              const loads = Object.keys(data.operationalProfile)
                .map(load => Number(load))
                .sort((a, b) => a - b)

              // Create chart data points
              const chartData = loads.map(load => {
                const dataPoint: any = {
                  load,
                  frequency: data.operationalProfile[load]
                }

                data.testSuites.forEach((suite: TestSuiteDto) => {
                  const testCase = suite.testCases.find((tc: TestCaseDto) => tc.load === load)
                  if (testCase && testCase.relativeDomainMetric !== undefined && testCase.relativeDomainMetric !== null) {
                    dataPoint[suite.systemUnderTest.name] = Number(testCase.relativeDomainMetric)
                  }
                })

                return dataPoint
              })

              const colors = ['#d83034', '#4ecb8d', '#ff73b6', '#f9e858', '#c701ff', '#008dff', '#ff9d3a']

              // Custom tooltip with rounded values
              const CustomTooltip = ({ active, payload, label }: any) => {
                if (active && payload && payload.length) {
                  return (
                    <div className="bg-background border border-border rounded-lg shadow-lg p-3">
                      <p className="font-medium mb-2">Load: {label} users</p>
                      {payload.map((entry: any, index: number) => {
                        if (entry.dataKey === 'frequency') {
                          return (
                            <p key={index} className="text-sm" style={{ color: entry.color }}>
                              Total Probability Mass: {Number(entry.value.toFixed(3))}
                            </p>
                          )
                        }
                        return (
                          <p key={index} className="text-sm" style={{ color: entry.color }}>
                            {entry.name}: {Number(entry.value?.toFixed(4)) ?? 'N/A'}
                          </p>
                        )
                      })}
                    </div>
                  )
                }
                return null
              }

              const maxRelativeDomainMetric = Math.max(...Object.values(data.operationalProfile).map(load => Number(load)))
              const maxY = Math.min(maxRelativeDomainMetric + 0.05, 1)

              return chartData.length > 0 ? (
                <ResponsiveContainer width="100%" height={400}>
                  <ComposedChart data={chartData} margin={{ left: 10, right: 10, top: 20, bottom: 20 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis
                      dataKey="load"
                      label={{ value: 'Load (users)', position: 'insideBottomLeft', offset: -10 }}
                      scale="point"
                    />
                    <YAxis
                      label={{ value: 'Relative Domain Metric', angle: -90, position: 'insideBottomLeft' }}
                      domain={[0, maxY]}
                      tickFormatter={(value) => value.toFixed(2)}
                    />
                    <Tooltip content={<CustomTooltip />} />
                    <Legend verticalAlign="top" align="right" layout="radial"/>
                    <Line
                      type="linear"
                      dataKey="frequency"
                      stroke="#003a7d"
                      strokeWidth={3}
                      dot={{ r: 2, fill: '#003a7d', fillOpacity: 1 }}
                      activeDot={{ r: 3 }}
                      name="Total Probability Mass"
                    />
                    {data.testSuites.map((suite: TestSuiteDto, index: number) => (
                      <Area
                        key={suite.id}
                        type="linear"
                        dataKey={suite.systemUnderTest.name}
                        stroke={colors[index % colors.length]}
                        fill={colors[index % colors.length]}
                        fillOpacity={0.2}
                        strokeWidth={2}
                        dot={{ r: 2, fill: colors[index % colors.length], fillOpacity: 1 }}
                        activeDot={{ r: 3 }}
                      />
                    ))}
                  </ComposedChart>
                </ResponsiveContainer>
              ) : (
                <p className="text-muted-foreground text-center py-8">No data available yet</p>
              )
            })()}
          </CardContent>
        </Card>

        {/* Scalability Footprint Radar Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Scalability Footprint</CardTitle>
          </CardHeader>
          <CardContent>
            {(() => {
              // Get all operations from test suite results
              const operations = new Set<string>()
              data.testSuites.forEach((suite: TestSuiteDto) => {
                if (suite.testSuiteResults?.operationExperimentResults) {
                  Object.keys(suite.testSuiteResults.operationExperimentResults).forEach(op => operations.add(op))
                }
              })

              if (operations.size === 0) {
                return <p className="text-muted-foreground text-center py-8">No operation results available yet</p>
              }

              // Create radar chart data points - one per operation
              const radarData = Array.from(operations).map(operationId => {
                const dataPoint: any = {
                  operation: operationId
                }

                data.testSuites.forEach((suite: TestSuiteDto) => {
                  const opResult = suite.testSuiteResults?.operationExperimentResults?.[operationId]
                  if (opResult?.scalabilityFootprint !== undefined && opResult.scalabilityFootprint !== null) {
                    dataPoint[suite.systemUnderTest.name] = Number(opResult.scalabilityFootprint)
                  }
                })

                return dataPoint
              })

                const colors = ['#d83034', '#4ecb8d', '#ff73b6', '#f9e858', '#c701ff', '#008dff', '#ff9d3a']

              return radarData.length > 0 ? (
                <ResponsiveContainer width="100%" height={400}>
                  <RadarChart data={radarData}>
                    <PolarGrid />
                    <PolarAngleAxis dataKey="operation" />
                    <PolarRadiusAxis angle={90} fontSize={10} stroke="#888" />
                    <Tooltip formatter={(value: any) => value ?? 'N/A'} />
                    <Legend verticalAlign="bottom" align="center" layout="horizontal"/>
                    {data.testSuites.map((suite: TestSuiteDto, index: number) => (
                      <Radar
                        key={suite.id}
                        name={suite.systemUnderTest.name}
                        dataKey={suite.systemUnderTest.name}
                        stroke={colors[index % colors.length]}
                        fill={colors[index % colors.length]}
                        fillOpacity={0.2}
                      />
                    ))}
                  </RadarChart>
                </ResponsiveContainer>
              ) : (
                <p className="text-muted-foreground text-center py-8">No data available yet</p>
              )
            })()}
          </CardContent>
        </Card>
      </div>

      {/* Operation Metrics Detail */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Operation Metrics</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-6">
            {/* Selection Controls */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">System Under Test</label>
                <Select value={selectedSutId} onValueChange={setSelectedSutId}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select system" />
                  </SelectTrigger>
                  <SelectContent>
                    {data.testSuites.map((suite: TestSuiteDto) => (
                      <SelectItem key={suite.systemUnderTest.id} value={suite.systemUnderTest.id}>
                        {suite.systemUnderTest.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium">Load Level</label>
                <Select value={selectedLoad} onValueChange={setSelectedLoad}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select load" />
                  </SelectTrigger>
                  <SelectContent>
                    {(() => {
                      const selectedSuite = data.testSuites.find(s => s.systemUnderTest.id === selectedSutId)
                      return selectedSuite?.testCases.sort((a, b) => a.load - b.load).map((tc: TestCaseDto) => (
                        <SelectItem key={tc.load} value={tc.load.toString()}>
                          {tc.load} users
                        </SelectItem>
                      ))
                    })()}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {(() => {
              const selectedSuite = data.testSuites.find(s => s.systemUnderTest.id === selectedSutId)
              const selectedTestCase = selectedSuite?.testCases.find(tc => tc.load.toString() === selectedLoad)

              if (!selectedSuite || !selectedTestCase) {
                return <p className="text-muted-foreground text-center py-8">Select a system and load to view metrics</p>
              }

              const operationMetrics = Object.values(selectedTestCase.operationMetrics)

              return (
                <div className="space-y-4">
                  {/* Summary Information */}
                  <div className="bg-muted/30 rounded-lg p-4">
                    <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
                      <div className="flex items-center gap-2">
                        <span className="text-muted-foreground">Status:</span>
                        <Badge variant={getStateColor(selectedTestCase.status)} className={`flex items-center gap-1 ${getStateClassName(selectedTestCase.status)}`}>
                          {getStateIcon(selectedTestCase.status)}
                          {selectedTestCase.status}
                        </Badge>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-muted-foreground">Load Frequency:</span>
                        <span className="font-mono font-medium">{Number(selectedTestCase.loadFrequency.toFixed(2))}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-muted-foreground">RDM:</span>
                        <span className="font-mono font-medium">
                          {selectedTestCase.relativeDomainMetric !== undefined && selectedTestCase.relativeDomainMetric !== null
                            ? Number(selectedTestCase.relativeDomainMetric.toFixed(4))
                            : 'N/A'}
                        </span>
                      </div>
                      {selectedTestCase.startTimestamp && (
                        <div className="flex items-center gap-2">
                          <span className="text-muted-foreground">Started:</span>
                          <span className="font-medium">{format(new Date(selectedTestCase.startTimestamp), 'Pp')}</span>
                        </div>
                      )}
                        {selectedTestCase.endTimestamp && (
                        <div className="flex items-center gap-2">
                          <span className="text-muted-foreground">Ended:</span>
                          <span className="font-medium">{format(new Date(selectedTestCase.endTimestamp), 'Pp')}</span>
                        </div>
                      )}
                      {selectedTestCase.startTimestamp && selectedTestCase.endTimestamp && (
                        <div className="flex items-center gap-2">
                          <span className="text-muted-foreground">Duration:</span>
                          <span className="font-medium">
                            {(() => {
                              const start = new Date(selectedTestCase.startTimestamp)
                              const end = new Date(selectedTestCase.endTimestamp)
                              const durationMs = end.getTime() - start.getTime()
                              const durationSec = Math.floor(durationMs / 1000)
                              const minutes = Math.floor(durationSec / 60)
                              const seconds = durationSec % 60
                              return minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`
                            })()}
                          </span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Operation Metrics Table */}
                  <div className="overflow-x-auto">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Operation</TableHead>
                          <TableHead className="text-right">Total Requests</TableHead>
                          <TableHead className="text-right">Failed Requests</TableHead>
                          <TableHead className="text-right">Std Dev (ms)</TableHead>
                          <TableHead className="text-right">P95 (ms)</TableHead>
                          <TableHead className="text-right">P99 (ms)</TableHead>
                          <TableHead className="text-right bg-blue-50 dark:bg-blue-950/30">Mean Response (ms)</TableHead>
                          <TableHead className="text-right bg-amber-50 dark:bg-amber-950/30">Scalability Threshold (ms)</TableHead>
                          <TableHead className="text-center bg-amber-50 dark:bg-amber-950/30">Passes Threshold</TableHead>
                          <TableHead className="text-right bg-amber-50 dark:bg-amber-950/30">Scalability Share</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {operationMetrics.map((metric) => (
                          <TableRow key={metric.operationId}>
                            <TableCell className="font-mono text-xs">{metric.operationId}</TableCell>
                            <TableCell className="text-right font-mono">{metric.totalRequests}</TableCell>
                            <TableCell className="text-right font-mono">{metric.failedRequests}</TableCell>
                            <TableCell className="text-right font-mono">{metric.stdDevResponseTimeMs.toFixed(2)}</TableCell>
                            <TableCell className="text-right font-mono">{metric.p95DurationMs.toFixed(2)}</TableCell>
                            <TableCell className="text-right font-mono">{metric.p99DurationMs.toFixed(2)}</TableCell>
                            <TableCell className="text-right font-mono bg-blue-50 dark:bg-blue-950/30">{metric.meanResponseTimeMs.toFixed(2)}</TableCell>
                            <TableCell className="text-right font-mono bg-amber-50 dark:bg-amber-950/30">{data.scalabilityThresholds ? data.scalabilityThresholds[metric.operationId].toFixed(2) : 'N/A'}</TableCell>
                            <TableCell className="text-center bg-amber-50 dark:bg-amber-950/30">
                              {metric.passScalabilityThreshold ? (
                                <CheckCircle className="h-4 w-4 text-green-600 inline" />
                              ) : (
                                <XCircle className="h-4 w-4 text-red-600 inline" />
                              )}
                            </TableCell>
                            <TableCell className="text-right font-mono bg-amber-50 dark:bg-amber-950/30">{metric.scalabilityShare.toFixed(4)}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>

                  {/* K6 Output Section */}
                  {selectedTestCase.k6Output && (
                    <div className="mt-6">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setShowK6Output(!showK6Output)}
                        className="mb-3"
                      >
                        {showK6Output ? 'Hide K6 Output' : 'Show K6 Output'}
                      </Button>

                      {showK6Output && (
                        <div className="bg-muted/50 rounded-lg p-4 overflow-auto max-h-96">
                          <pre className="text-xs font-mono whitespace-pre-wrap">{selectedTestCase.k6Output}</pre>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )
            })()}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
