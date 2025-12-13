import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import type { TestCaseDto, TestSuiteDto } from '@/api/generated/openAPIDefinition.schemas'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ArrowLeft, Loader2, CheckCircle, XCircle, Clock, PlayCircle, Activity, RotateCcw } from 'lucide-react'
import { format } from 'date-fns'
import {useGetBenchmarkRun, useRestartBenchmarkRun} from "@/api/generated/benchmark-run-controller/benchmark-run-controller.ts";
import { useGetBenchmark } from "@/api/generated/benchmark-controller/benchmark-controller.ts";
import { useToast } from '@/components/ui/use-toast'
import { useQueryClient } from '@tanstack/react-query'
import { Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ComposedChart } from 'recharts'

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
  const { data: benchmarkData } = useGetBenchmark(data?.benchmarkId || '', {
    query: {
      enabled: !!data?.benchmarkId,
    },
  })

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
  const totalTestSuites = data.testSuites?.length || 0
  const completedTestSuites = data.testSuites?.filter((suite: TestSuiteDto) => suite.status === 'COMPLETED').length || 0
  const failedTestSuites = data.testSuites?.filter((suite: TestSuiteDto) => suite.status === 'FAILED').length || 0

  // Count total operations from first suite with results
  const totalOperations = data.testSuites?.find(suite => suite.testSuiteResults?.operationExperimentResults)
    ?.testSuiteResults?.operationExperimentResults
    ? Object.keys(data.testSuites.find(suite => suite.testSuiteResults?.operationExperimentResults)!.testSuiteResults!.operationExperimentResults).length
    : 0

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
              <p className="text-sm font-medium text-muted-foreground">Number of Tested Architectures</p>
              <p className="text-3xl font-bold mt-2">{totalTestSuites}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm font-medium text-muted-foreground">Completed</p>
              <p className="text-3xl font-bold mt-2 text-green-600">{completedTestSuites}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm font-medium text-muted-foreground">Failed</p>
              <p className="text-3xl font-bold mt-2 text-red-600">{failedTestSuites}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm font-medium text-muted-foreground">Operations</p>
              <p className="text-3xl font-bold mt-2">{totalOperations}</p>
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

      {/* Domain Metric Chart */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Domain Metric Across Load Levels</CardTitle>
        </CardHeader>
        <CardContent>
          {(() => {
            if (!benchmarkData?.operationalSetting?.operationalProfile) {
              return <p className="text-muted-foreground text-center py-8">No operational profile available</p>
            }

            // Get all loads from operational profile
            const loads = Object.keys(benchmarkData.operationalSetting.operationalProfile)
              .map(load => Number(load))
              .sort((a, b) => a - b)

            // Create chart data points
            const chartData = loads.map(load => {
              const dataPoint: any = {
                load,
                frequency: benchmarkData.operationalSetting.operationalProfile[load]
              }

              data.testSuites.forEach((suite: TestSuiteDto) => {
                const testCase = suite.testCases.find((tc: TestCaseDto) => tc.load === load)
                if (testCase && testCase.relativeDomainMetric !== undefined && testCase.relativeDomainMetric !== null) {
                  dataPoint[suite.systemUnderTest.name] = Number(testCase.relativeDomainMetric)
                }
              })

              return dataPoint
            })

            const colors = ['#22c55e', '#3b82f6', '#f59e0b', '#ec4899', '#8b5cf6', '#14b8a6']

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
                            Frequency: {entry.value.toFixed(2)}
                          </p>
                        )
                      }
                      return (
                        <p key={index} className="text-sm" style={{ color: entry.color }}>
                          {entry.name}: {entry.value?.toFixed(4) ?? 'N/A'}
                        </p>
                      )
                    })}
                  </div>
                )
              }
              return null
            }

            return chartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={400}>
                <ComposedChart data={chartData} margin={{ left: 10, right: 10 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="load"
                    label={{ value: 'Load (users)', position: 'insideBottomLeft', offset: -10 }}
                    scale="point"
                  />
                  <YAxis
                    label={{ value: 'Relative Domain Metric', angle: -90, position: 'insideBottomLeft' }}
                    domain={[0, 1]}
                    tickFormatter={(value) => value.toFixed(2)}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="frequency"
                    stroke="blue"
                    strokeWidth={2}
                    dot={{ r: 3 }}
                    activeDot={{ r: 5 }}
                    name="Frequency"
                  />
                  {data.testSuites.map((suite: TestSuiteDto, index: number) => (
                    <Line
                      key={suite.id}
                      type="monotone"
                      dataKey={suite.systemUnderTest.name}
                      stroke={colors[index % colors.length]}
                      strokeWidth={2}
                      dot={{ r: 4 }}
                      activeDot={{ r: 6 }}
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

      {/* Test Suites Detail */}
      {data.testSuites.map((suite: TestSuiteDto) => (
        <Card key={suite.id}>
          <CardHeader>
            <div className="flex items-center gap-2">
              <CardTitle className="text-base">{suite.systemUnderTest.name}</CardTitle>
              <Badge variant={getStateColor(suite.status)} className={`flex items-center gap-1 ${getStateClassName(suite.status)}`}>
                {getStateIcon(suite.status)}
                {suite.status}
              </Badge>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="text-sm text-muted-foreground">
                {suite.systemUnderTest.description && (
                  <p className="mb-2"><span className="font-medium">Description:</span> {suite.systemUnderTest.description}</p>
                )}
                <p><span className="font-medium">System ID:</span> <span className="font-mono text-xs">{suite.systemUnderTest.id}</span></p>
                <p><span className="font-medium">Number of Test Cases:</span> {suite.testCases.length}</p>
                {suite.startTimestamp && <p><span className="font-medium">Started:</span> {format(new Date(suite.startTimestamp), 'PPp')}</p>}
                {suite.endTimestamp && <p><span className="font-medium">Completed:</span> {format(new Date(suite.endTimestamp), 'PPp')}</p>}
              </div>

              {suite.testCases.length > 0 && (
                <div>
                  <h4 className="text-sm font-semibold mb-2">Test Cases by Load</h4>
                  <div className="overflow-x-auto">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Load (users)</TableHead>
                          <TableHead>Status</TableHead>
                          <TableHead className="text-right">RDM</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {suite.testCases.sort((a, b) => a.load - b.load).map((testCase: TestCaseDto) => (
                          <TableRow key={testCase.id}>
                            <TableCell className="font-mono">{testCase.load}</TableCell>
                            <TableCell>
                              <Badge variant={getStateColor(testCase.status)} className={`flex items-center gap-1 w-fit ${getStateClassName(testCase.status)}`}>
                                {getStateIcon(testCase.status)}
                                {testCase.status}
                              </Badge>
                            </TableCell>
                            <TableCell className="text-right font-mono">
                              {testCase.relativeDomainMetric !== undefined && testCase.relativeDomainMetric !== null
                                ? testCase.relativeDomainMetric.toFixed(4)
                                : 'N/A'}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
