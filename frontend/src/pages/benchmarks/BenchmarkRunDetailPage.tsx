import { useState, useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import type { TargetTestCaseDto } from '@/api/generated/openAPIDefinition.schemas'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ArrowLeft, Loader2, CheckCircle, XCircle, Clock, PlayCircle, Activity, ChevronDown, ChevronUp, Info, RotateCcw } from 'lucide-react'
import { format } from 'date-fns'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, Legend, ResponsiveContainer, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar, ComposedChart, Scatter, ReferenceLine } from 'recharts'
import {useGetBenchmarkRun, useRestartBenchmarkRun} from "@/api/generated/benchmark-run-controller/benchmark-run-controller.ts";
import {useGetBenchmark} from "@/api/generated/benchmark-controller/benchmark-controller.ts";
import { useToast } from '@/components/ui/use-toast'
import { useQueryClient } from '@tanstack/react-query'

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
  const { data, isLoading, error } = useGetBenchmarkRun(runId!)
  const { data: benchmarkData, isLoading: isBenchmarkLoading } = useGetBenchmark(benchmarkId!)
  const [expandedK6Outputs, setExpandedK6Outputs] = useState<Set<string>>(new Set())
  const [selectedLoad, setSelectedLoad] = useState<string>('')

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

  // Prepare chart data for Domain Metric comparison
  const getDomainMetricChartData = () => {
    if (!data?.baselineTestCase || !data?.targetTestCases || !benchmarkData?.operationalSetting?.operationalProfile) {
      return []
    }

    // Get all loads from operational profile
    const loads = Object.keys(benchmarkData.operationalSetting.operationalProfile)
      .map(load => Number(load))
      .sort((a, b) => a - b)

    // Create chart data points
    return loads.map(load => {
      const baselineRDM = data.baselineTestCase.relativeDomainMetrics[load]
      const targetTestCase = data.targetTestCases.find((tc: TargetTestCaseDto) => tc.load === load)

      return {
        load,
        baseline: baselineRDM != null ? Number(baselineRDM) : null,
        target: targetTestCase?.relativeDomainMetric != null ? Number(targetTestCase.relativeDomainMetric) : null
      }
    })
  }

  const chartData = getDomainMetricChartData()

  // Calculate dynamic Y axis domain for domain metric chart
  const getDomainMetricYAxisDomain = () => {
    if (chartData.length === 0) return [0, 1]

    const allValues = chartData
      .flatMap(d => [d.baseline, d.target])
      .filter((v): v is number => v !== null && v !== undefined)

    if (allValues.length === 0) return [0, 1]

    const maxValue = Math.max(...allValues)
    const minValue = Math.min(...allValues)

    // Add 10% padding on top, and start from 0 or slightly below min
    const padding = (maxValue - minValue) * 0.1
    const yMax = maxValue + padding
    const yMin = Math.max(0, minValue - padding)

    return [yMin, yMax]
  }

  const yAxisDomain = getDomainMetricYAxisDomain()

  // Prepare radar chart data for scalability footprints
  const getScalabilityFootprintRadarData = () => {
    if (!data?.experimentResults?.operationExperimentResults) {
      return []
    }

    // Create radar data: each operation is a point on the radar
    return Object.entries(data.experimentResults.operationExperimentResults).map(([operationId, result]: [string, any]) => {
      const scalabilityFootprint = result.scalabilityFootprint

      return {
        operation: operationId,
        gsl: scalabilityFootprint !== null && scalabilityFootprint !== undefined
          ? Math.round(scalabilityFootprint)
          : 0
      }
    })
  }

  const radarData = getScalabilityFootprintRadarData()

  // Prepare load-based bar chart data for scalability gap
  const getScalabilityGapLoadData = () => {
    if (!data?.experimentResults?.operationExperimentResults || !benchmarkData?.operationalSetting?.operationalProfile) {
      return { chartData: [], loads: [] }
    }

    // Get all loads from operational profile
    const loads = Object.keys(benchmarkData.operationalSetting.operationalProfile)
      .map(load => Number(load))
      .sort((a, b) => a - b)

    // Get max load to determine if we should show N/A
    const maxLoad = loads.length > 0 ? Math.max(...loads) : null

    // Create data structure: for each operation, include bars at all loads > GSL
    const chartData: Array<{operation: string, load: number, value: number, loadStr: string}> = []

    Object.entries(data.experimentResults.operationExperimentResults).forEach(([operationId, result]: [string, any]) => {
      const scalabilityFootprint = result.scalabilityFootprint
      const scalabilityGap = result.scalabilityGap

      // Skip if operation always fails or never fails
      const shouldShowNA = scalabilityFootprint === null || scalabilityFootprint === undefined ||
                          (maxLoad !== null && scalabilityFootprint === maxLoad)

      if (!shouldShowNA) {
        // Add bar at the first load after GSL
        const nextLoad = loads.find(load => load > scalabilityFootprint)
        if (nextLoad !== undefined) {
          chartData.push({
            operation: operationId,
            load: nextLoad,
            value: scalabilityGap ?? 0, // Use 0 if null/undefined
            loadStr: `${nextLoad}`
          })
        }
      }
    })

    return { chartData, loads }
  }

  // Prepare load-based bar chart data for performance offset
  const getPerformanceOffsetLoadData = () => {
    if (!data?.experimentResults?.operationExperimentResults || !benchmarkData?.operationalSetting?.operationalProfile) {
      return { chartData: [], loads: [] }
    }

    // Get all loads from operational profile
    const loads = Object.keys(benchmarkData.operationalSetting.operationalProfile)
      .map(load => Number(load))
      .sort((a, b) => a - b)

    // Get max load to determine if we should show N/A
    const maxLoad = loads.length > 0 ? Math.max(...loads) : null

    // Create data structure: for each operation, include bars at all loads > GSL
    const chartData: Array<{operation: string, load: number, value: number, loadStr: string}> = []

    Object.entries(data.experimentResults.operationExperimentResults).forEach(([operationId, result]: [string, any]) => {
      const scalabilityFootprint = result.scalabilityFootprint
      const performanceOffset = result.performanceOffset

      // Skip if operation always fails or never fails
      const shouldShowNA = scalabilityFootprint === null || scalabilityFootprint === undefined ||
                          (maxLoad !== null && scalabilityFootprint === maxLoad)

      if (!shouldShowNA) {
        // Add bar at the first load after GSL
        const nextLoad = loads.find(load => load > scalabilityFootprint)
        if (nextLoad !== undefined) {
          chartData.push({
            operation: operationId,
            load: nextLoad,
            value: performanceOffset ?? 0, // Use 0 if null/undefined
            loadStr: `${nextLoad}`
          })
        }
      }
    })

    return { chartData, loads }
  }

  const scalabilityGapLoadData = getScalabilityGapLoadData()
  const performanceOffsetLoadData = getPerformanceOffsetLoadData()

  // Transform load data into chart-ready format with operations on X-axis
  // Each data point represents a bar at a specific load level
  const transformToChartData = (loadData: { chartData: Array<{operation: string, load: number, value: number, loadStr: string}>, loads: number[] }) => {
    const { chartData, loads } = loadData

    if (chartData.length === 0) {
      return { formattedData: [], loads: [], rawData: [], loadIndexMap: new Map() }
    }

    // Create a map of load value to index for equal spacing
    const loadIndexMap = new Map<number, number>()
    loads.forEach((load, index) => {
      loadIndexMap.set(load, index)
    })

    // Get unique operations
    const operations = Array.from(new Set(chartData.map(d => d.operation))).sort()

    // Create one data point per operation with load-specific values
    const formattedData = operations.map(operation => {
      const opData: any = { operation }

      loads.forEach(load => {
        const dataPoints = chartData.filter(d => d.operation === operation && d.load === load)
        if (dataPoints.length > 0) {
          // Store both the value and the load position using index for equal spacing
          opData[`load_${load}`] = loadIndexMap.get(load)!  // Y position is the index (equal spacing)
          opData[`value_${load}`] = dataPoints[0].value  // Store the percentage value separately
        }
      })

      return opData
    })

    return { formattedData, loads, rawData: chartData, loadIndexMap }
  }

  const scalabilityGapChartData = transformToChartData(scalabilityGapLoadData)
  const performanceOffsetChartData = transformToChartData(performanceOffsetLoadData)

  // Calculate max load from operational profile for radar chart domain
  const getMaxLoad = () => {
    if (!benchmarkData?.operationalSetting?.operationalProfile) {
      return 'auto'
    }
    const loads = Object.keys(benchmarkData.operationalSetting.operationalProfile).map(load => Number(load))
    return loads.length > 0 ? Math.max(...loads) : 'auto'
  }

  const maxLoad = getMaxLoad()

  if (isLoading || isBenchmarkLoading) {
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
  const completedTestCases = data.targetTestCases?.filter((tc: TargetTestCaseDto) => tc.status === 'COMPLETED').length || 0
  const failedTestCases = data.targetTestCases?.filter((tc: TargetTestCaseDto) => tc.status === 'FAILED').length || 0

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
        {data.experimentResults ? (
          <Card className="border-2 border-green-200 dark:border-green-800">
            <CardContent className="pt-6">
              <div className="p-6 rounded-lg bg-gradient-to-r from-green-50 to-emerald-50 dark:from-green-950/30 dark:to-emerald-950/30">
                <div className="flex flex-col items-center justify-center gap-4">
                  <div className="text-center">
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
        ) : data.status === 'FAILED' ? (
          <Card className="border-2 border-destructive/50">
            <CardContent className="pt-6">
              <div className="p-6 rounded-lg bg-destructive/10 border border-destructive/30 flex flex-col items-center gap-3 text-center">
                <XCircle className="h-10 w-10 text-destructive" />
                <div>
                  <p className="text-lg font-bold text-destructive">Total Domain Metric unavailable</p>
                  <p className="text-sm text-muted-foreground">
                    The benchmark run failed before the Total Domain Metric could be calculated.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        ) : (
          <Card className="border-2 border-muted">
            <CardContent className="pt-6">
              <div className="p-6 rounded-lg bg-muted/20">
                <div className="flex flex-col items-center justify-center gap-4">
                  <div className="text-center">
                    <p className="text-lg font-bold text-muted-foreground mb-2">Total Domain Metric (TDM)</p>
                    <p className="text-sm text-muted-foreground">Overall scalability of the System Under Test</p>
                  </div>
                  <div className="text-center">
                    <div className="flex items-center justify-center gap-3">
                      <Loader2 className="h-10 w-10 animate-spin text-muted-foreground" />
                      <p className="text-3xl font-bold text-muted-foreground">Calculating...</p>
                    </div>
                    <p className="text-sm text-muted-foreground mt-2 font-medium">
                      Waiting for all test cases to complete
                    </p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Domain Metric Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Relative Domain Metrics over Load</CardTitle>
          </CardHeader>
          <CardContent>
            {chartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={400}>
                <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="load"
                    label={{ value: 'Load (users)', position: 'insideBottomLeft', offset: -20}}
                  />
                  <YAxis
                    label={{ value: 'Domain Metric', angle: -90, position: 'insideBottomLeft' }}
                    domain={yAxisDomain}
                    tickFormatter={(value) => value.toFixed(2)}
                  />
                  <RechartsTooltip
                    formatter={(value: any) => value !== null ? value.toFixed(4) : 'N/A'}
                    labelFormatter={(label) => `Load: ${label} users`}
                  />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="baseline"
                    stroke="#3b82f6"
                    strokeWidth={2}
                    name="Baseline"
                    connectNulls
                    dot={{ r: 4 }}
                  />
                  <Line
                    type="monotone"
                    dataKey="target"
                    stroke="#a855f7"
                    strokeWidth={2}
                    name="Target"
                    connectNulls
                    dot={{ r: 4 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-[400px] flex items-center justify-center border-2 border-dashed border-muted rounded-lg bg-muted/20">
                <p className="text-sm text-muted-foreground">
                  {!benchmarkData ? 'Loading benchmark data...' : 'No data available for chart'}
                </p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Scalability Footprint Radar Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Scalability Footprint by Operation (Raw GSL)</CardTitle>
          </CardHeader>
          <CardContent>
            {radarData.length > 0 ? (
              <ResponsiveContainer width="100%" height={400}>
                <RadarChart data={radarData}>
                  <PolarGrid />
                  <PolarAngleAxis dataKey="operation" tick={{ fontSize: 11 }} />
                  <PolarRadiusAxis angle={90} domain={[0, maxLoad]} tick={{ fontSize: 10 }} />
                  <Radar
                    name="GSL (users)"
                    dataKey="gsl"
                    stroke="#10b981"
                    fill="#10b981"
                    fillOpacity={0.3}
                  />
                  <Legend />
                  <RechartsTooltip
                    formatter={(value: any) => `${value} users`}
                    labelFormatter={(label) => `Operation: ${label}`}
                  />
                </RadarChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-[400px] flex items-center justify-center border-2 border-dashed border-muted rounded-lg bg-muted/20">
                <p className="text-sm text-muted-foreground">
                  {!data?.experimentResults ? 'No experiment results available' : 'No operation data available for chart'}
                </p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Scalability Gap Load-based Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Scalability Gap by Operation</CardTitle>
          </CardHeader>
          <CardContent>
            {scalabilityGapChartData.formattedData.length > 0 ? (
              <ResponsiveContainer width="100%" height={400}>
                <ComposedChart data={scalabilityGapChartData.formattedData} margin={{ top: 20, right: 30, left: 60, bottom: 40 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="operation"
                    angle={-45}
                    textAnchor="end"
                    height={80}
                    tick={{ fontSize: 11 }}
                    padding={{ left: 30, right: 30 }}
                  />
                  <YAxis
                    label={{ value: 'Load (users)', angle: -90, position: 'insideLeft' }}
                    domain={scalabilityGapChartData.loads.length > 0 ? [-0.5, scalabilityGapChartData.loads.length + 1.5] : [0, 'auto']}
                    ticks={scalabilityGapChartData.loads.map((_, idx) => idx)}
                    tickFormatter={(value) => {
                      const load = scalabilityGapChartData.loads[value]
                      return load !== undefined ? load.toString() : ''
                    }}
                    type="number"
                  />
                  <RechartsTooltip
                    content={({ active, payload }: any) => {
                      if (active && payload && payload.length) {
                        const data = payload[0].payload
                        return (
                          <div className="bg-background border rounded p-2 shadow-lg">
                            <p className="font-semibold">{data.operation}</p>
                            {scalabilityGapChartData.loads.map(load => {
                              const value = data[`value_${load}`]
                              if (value !== undefined) {
                                return (
                                  <p key={load} className="text-sm">
                                    Load {load}: {(value * 100).toFixed(0)}%
                                  </p>
                                )
                              }
                              return null
                            })}
                          </div>
                        )
                      }
                      return null
                    }}
                  />
                  {/* Add reference lines for each load level */}
                  {scalabilityGapChartData.loads.map((load, idx) => (
                    <ReferenceLine key={load} y={idx} stroke="#94a3b8" strokeDasharray="3 3" />
                  ))}
                  {/* Add scatter points for each load level */}
                  {scalabilityGapChartData.loads.map((load, idx) => (
                    <Scatter
                      key={load}
                      dataKey={`load_${load}`}
                      fill={`hsl(${30 + idx * 40}, 85%, 60%)`}
                      shape={(props: any) => {
                        const { cx, cy, payload } = props
                        const percentageValue = payload[`value_${load}`]
                        if (percentageValue === undefined) return <g />

                        // Calculate bar height based on percentage value
                        // Scale the percentage to a visible height on the chart
                        const barHeight = percentageValue * 20 // Scale factor for visual height (reduced for smaller bars)
                        const barWidth = 12

                        // For zero values, show a small marker instead of a bar
                        if (percentageValue === 0) {
                          return (
                            <g>
                              <circle
                                cx={cx}
                                cy={cy}
                                r={3}
                                fill="#94a3b8"
                                stroke="#64748b"
                                strokeWidth={1}
                              />
                              <text
                                x={cx}
                                y={cy - 8}
                                textAnchor="middle"
                                fontSize={10}
                                fill="#64748b"
                              >
                                0%
                              </text>
                            </g>
                          )
                        }

                        return (
                          <g>
                            <rect
                              x={cx - barWidth / 2}
                              y={cy - barHeight}
                              width={barWidth}
                              height={barHeight}
                              fill={`hsl(${30 + idx * 40}, 85%, 60%)`}
                              stroke={`hsl(${30 + idx * 40}, 85%, 40%)`}
                              strokeWidth={1}
                            />
                            <text
                              x={cx}
                              y={cy - barHeight - 5}
                              textAnchor="middle"
                              fontSize={10}
                              fill="#374151"
                            >
                              {(percentageValue * 100).toFixed(0)}%
                            </text>
                          </g>
                        )
                      }}
                    />
                  ))}
                </ComposedChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-[400px] flex items-center justify-center border-2 border-dashed border-muted rounded-lg bg-muted/20">
                <p className="text-sm text-muted-foreground">
                  {!data?.experimentResults ? 'No experiment results available' : 'No scalability gap data available'}
                </p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Performance Offset Load-based Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Performance Offset by Operation</CardTitle>
          </CardHeader>
          <CardContent>
            {performanceOffsetChartData.formattedData.length > 0 ? (
              <ResponsiveContainer width="100%" height={400}>
                <ComposedChart data={performanceOffsetChartData.formattedData} margin={{ top: 20, right: 30, left: 60, bottom: 40 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="operation"
                    angle={-45}
                    textAnchor="end"
                    height={80}
                    tick={{ fontSize: 11 }}
                    padding={{ left: 30, right: 30 }}
                  />
                  <YAxis
                    label={{ value: 'Load (users)', angle: -90, position: 'insideLeft' }}
                    domain={performanceOffsetChartData.loads.length > 0 ? [-0.5, performanceOffsetChartData.loads.length + 1.5] : [0, 'auto']}
                    ticks={performanceOffsetChartData.loads.map((_, idx) => idx)}
                    tickFormatter={(value) => {
                      const load = performanceOffsetChartData.loads[value]
                      return load !== undefined ? load.toString() : ''
                    }}
                    type="number"
                  />
                  <RechartsTooltip
                    content={({ active, payload }: any) => {
                      if (active && payload && payload.length) {
                        const data = payload[0].payload
                        return (
                          <div className="bg-background border rounded p-2 shadow-lg">
                            <p className="font-semibold">{data.operation}</p>
                            {performanceOffsetChartData.loads.map(load => {
                              const value = data[`value_${load}`]
                              if (value !== undefined) {
                                return (
                                  <p key={load} className="text-sm">
                                    Load {load}: {(value * 100).toFixed(0)}%
                                  </p>
                                )
                              }
                              return null
                            })}
                          </div>
                        )
                      }
                      return null
                    }}
                  />
                  {/* Add reference lines for each load level */}
                  {performanceOffsetChartData.loads.map((load, idx) => (
                    <ReferenceLine key={load} y={idx} stroke="#94a3b8" strokeDasharray="3 3" />
                  ))}
                  {/* Add scatter points for each load level */}
                  {performanceOffsetChartData.loads.map((load, idx) => (
                    <Scatter
                      key={load}
                      dataKey={`load_${load}`}
                      fill={`hsl(${260 + idx * 30}, 75%, 60%)`}
                      shape={(props: any) => {
                        const { cx, cy, payload } = props
                        const percentageValue = payload[`value_${load}`]
                        if (percentageValue === undefined) return <g />

                        // Calculate bar height based on percentage value
                        // Scale the percentage to a visible height on the chart
                        const barHeight = percentageValue * 20 // Scale factor for visual height (reduced for smaller bars)
                        const barWidth = 12

                        // For zero values, show a small marker instead of a bar
                        if (percentageValue === 0) {
                          return (
                            <g>
                              <circle
                                cx={cx}
                                cy={cy}
                                r={3}
                                fill="#94a3b8"
                                stroke="#64748b"
                                strokeWidth={1}
                              />
                              <text
                                x={cx}
                                y={cy - 8}
                                textAnchor="middle"
                                fontSize={10}
                                fill="#64748b"
                              >
                                0%
                              </text>
                            </g>
                          )
                        }

                        return (
                          <g>
                            <rect
                              x={cx - barWidth / 2}
                              y={cy - barHeight}
                              width={barWidth}
                              height={barHeight}
                              fill={`hsl(${260 + idx * 30}, 75%, 60%)`}
                              stroke={`hsl(${260 + idx * 30}, 75%, 40%)`}
                              strokeWidth={1}
                            />
                            <text
                              x={cx}
                              y={cy - barHeight - 5}
                              textAnchor="middle"
                              fontSize={10}
                              fill="#374151"
                            >
                              {(percentageValue * 100).toFixed(0)}%
                            </text>
                          </g>
                        )
                      }}
                    />
                  ))}
                </ComposedChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-[400px] flex items-center justify-center border-2 border-dashed border-muted rounded-lg bg-muted/20">
                <p className="text-sm text-muted-foreground">
                  {!data?.experimentResults ? 'No experiment results available' : 'No performance offset data available'}
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Operation Experiment Results Summary */}
      {data.experimentResults && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Operation Results Summary</CardTitle>
          </CardHeader>
          <CardContent>

            {data.experimentResults.operationExperimentResults && Object.keys(data.experimentResults.operationExperimentResults).length > 0 && (() => {
              // Calculate max load from target test cases
              const maxLoad = data.targetTestCases && data.targetTestCases.length > 0
                ? Math.max(...data.targetTestCases.map((tc: TargetTestCaseDto) => tc.load))
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

      {/* Test Cases Comparison */}
      <Card>
        <CardHeader>
          <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
            <div className="flex items-center gap-3 flex-wrap">
              <CardTitle className="text-base">Test Case Comparison</CardTitle>
            </div>
            {data.targetTestCases && data.targetTestCases.length > 0 && (
              <div className="flex items-center gap-2">
                <span className="text-sm text-muted-foreground">Select Target Load:</span>
                <Select value={selectedLoad} onValueChange={setSelectedLoad}>
                  <SelectTrigger className="w-[140px]">
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
        <CardContent className="space-y-6">
          {/* Test Case Info */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Baseline Info */}
            {data.baselineTestCase && (
              <div className="p-4 rounded-lg border bg-blue-50/50 dark:bg-blue-950/20">
                <div className="flex items-center gap-2 mb-3">
                  <Badge variant="default" className="bg-blue-600">Baseline</Badge>
                  <span className="text-sm font-medium">{data.baselineTestCase.load} users</span>
                  <Badge variant={getStateColor(data.baselineTestCase.status)} className={`flex items-center gap-1 ${getStateClassName(data.baselineTestCase.status)}`}>
                    {getStateIcon(data.baselineTestCase.status)}
                    {data.baselineTestCase.status}
                  </Badge>
                </div>
                <p className="text-xs font-mono text-muted-foreground mb-2">{data.baselineTestCase.baselineSutId}</p>
                {data.baselineTestCase.startTimestamp && data.baselineTestCase.endTimestamp && (
                  <p className="text-xs text-muted-foreground">
                    {format(new Date(data.baselineTestCase.startTimestamp), 'PPp')} - {format(new Date(data.baselineTestCase.endTimestamp), 'p')}
                  </p>
                )}
              </div>
            )}

            {/* Target Info */}
            {data.targetTestCases && data.targetTestCases.length > 0 && (() => {
              const testCase = data.targetTestCases.find((tc: TargetTestCaseDto) => tc.load.toString() === selectedLoad)
              if (!testCase) return null

              // Get the operational profile frequency for this load
              const loadFrequency = benchmarkData?.operationalSetting?.operationalProfile?.[testCase.load]

              return (
                <div className="p-4 rounded-lg border bg-purple-50/50 dark:bg-purple-950/20">
                  <div className="flex items-center gap-2 mb-3 flex-wrap">
                    <Badge variant="default" className="bg-purple-600">Target</Badge>
                    <span className="text-sm font-medium">{testCase.load} users</span>
                    {testCase.loadFrequency !== undefined && (
                      <span className="text-xs text-muted-foreground">
                        ({(testCase.loadFrequency * 100).toFixed(0)}% probability)
                      </span>
                    )}
                    <Badge variant={getStateColor(testCase.status)} className={`text-xs ${getStateClassName(testCase.status)}`}>
                      {getStateIcon(testCase.status)}
                      {testCase.status}
                    </Badge>
                  </div>
                  {testCase.startTimestamp && testCase.endTimestamp && (
                    <p className="text-xs text-muted-foreground mb-2">
                      {format(new Date(testCase.startTimestamp), 'PPp')} - {format(new Date(testCase.endTimestamp), 'p')}
                    </p>
                  )}
                  {/* Relative Domain Metric */}
                  {testCase.relativeDomainMetric !== undefined && (
                    <div className="mt-3 p-3 rounded-lg bg-blue-100/50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-800">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-xs font-semibold text-blue-900 dark:text-blue-100">RDM</p>
                          <p className="text-xs text-muted-foreground">Relative Domain Metric</p>
                        </div>
                        <p className="text-2xl font-bold text-blue-700 dark:text-blue-400">
                          {Number(testCase.relativeDomainMetric.toFixed(4))} / {loadFrequency !== undefined ? Number(loadFrequency) : 'N/A'}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              )
            })()}
          </div>

          {/* Combined Operation Metrics Table */}
          {data.baselineTestCase?.operationMetrics && data.targetTestCases && data.targetTestCases.length > 0 && (() => {
            const testCase = data.targetTestCases.find((tc: TargetTestCaseDto) => tc.load.toString() === selectedLoad)
            if (!testCase?.operationMetrics) return null

            // Get all unique operation IDs
            const baselineOps = Object.keys(data.baselineTestCase.operationMetrics || {})
            const targetOps = Object.keys(testCase.operationMetrics || {})
            const allOperationIds = Array.from(new Set([...baselineOps, ...targetOps])).sort()

            return (
              <div className="space-y-3">
                <p className="text-sm font-semibold">Operation Metrics Comparison</p>
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="py-2 align-middle border-r">Operation ID</TableHead>
                        <TableHead colSpan={2} className="text-center py-2 text-xs border-r">Total Req</TableHead>
                        <TableHead colSpan={2} className="text-center py-2 text-xs border-r">Failed</TableHead>
                        <TableHead colSpan={2} className="text-center py-2 text-xs border-r">Mean (ms)</TableHead>
                        <TableHead colSpan={2} className="text-center py-2 text-xs border-r">Std Dev</TableHead>
                        <TableHead colSpan={2} className="text-center py-2 text-xs border-r">P95</TableHead>
                        <TableHead colSpan={2} className="text-center py-2 text-xs border-r">P99</TableHead>
                        <TableHead colSpan={2} className="text-center py-2 text-xs border-r">Threshold / Status</TableHead>
                        <TableHead className="text-center py-2 text-xs">Share</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {allOperationIds.map((operationId) => {
                        const baselineMetric = data.baselineTestCase?.operationMetrics?.[operationId]
                        const targetMetric = testCase.operationMetrics?.[operationId]

                        return (
                          <TableRow key={operationId}>
                            <TableCell className="font-mono text-xs font-medium py-2 border-r">{operationId}</TableCell>

                            {/* Total Requests - Baseline */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-blue-50 dark:bg-blue-950/30">
                              {baselineMetric ? baselineMetric.totalRequests : 'N/A'}
                            </TableCell>
                            {/* Total Requests - Target */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-purple-50 dark:bg-purple-950/30 border-r">
                              {targetMetric ? targetMetric.totalRequests : 'N/A'}
                            </TableCell>

                            {/* Failed Requests - Baseline */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-blue-50 dark:bg-blue-950/30">
                              {baselineMetric ? baselineMetric.failedRequests : 'N/A'}
                            </TableCell>
                            {/* Failed Requests - Target */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-purple-50 dark:bg-purple-950/30 border-r">
                              {targetMetric ? targetMetric.failedRequests : 'N/A'}
                            </TableCell>

                            {/* Mean Response Time - Baseline */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-blue-50 dark:bg-blue-950/30">
                              {baselineMetric ? (
                                <span className="font-semibold text-amber-600 dark:text-amber-400">
                                  {Number(baselineMetric.meanResponseTimeMs).toFixed(2)}
                                </span>
                              ) : 'N/A'}
                            </TableCell>
                            {/* Mean Response Time - Target */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-purple-50 dark:bg-purple-950/30 border-r">
                              {targetMetric ? (
                                <span className="font-semibold text-amber-600 dark:text-amber-400">
                                  {Number(targetMetric.meanResponseTimeMs).toFixed(2)}
                                </span>
                              ) : 'N/A'}
                            </TableCell>

                            {/* Std Dev - Baseline */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-blue-50 dark:bg-blue-950/30">
                              {baselineMetric ? Number(baselineMetric.stdDevResponseTimeMs).toFixed(2) : 'N/A'}
                            </TableCell>
                            {/* Std Dev - Target */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-purple-50 dark:bg-purple-950/30 border-r">
                              {targetMetric ? Number(targetMetric.stdDevResponseTimeMs).toFixed(2) : 'N/A'}
                            </TableCell>

                            {/* P95 - Baseline */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-blue-50 dark:bg-blue-950/30">
                              {baselineMetric ? Number(baselineMetric.p95DurationMs).toFixed(2) : 'N/A'}
                            </TableCell>
                            {/* P95 - Target */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-purple-50 dark:bg-purple-950/30 border-r">
                              {targetMetric ? Number(targetMetric.p95DurationMs).toFixed(2) : 'N/A'}
                            </TableCell>

                            {/* P99 - Baseline */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-blue-50 dark:bg-blue-950/30">
                              {baselineMetric ? Number(baselineMetric.p99DurationMs).toFixed(2) : 'N/A'}
                            </TableCell>
                            {/* P99 - Target */}
                            <TableCell className="text-right font-mono text-xs py-2 bg-purple-50 dark:bg-purple-950/30 border-r">
                              {targetMetric ? Number(targetMetric.p99DurationMs).toFixed(2) : 'N/A'}
                            </TableCell>

                            {/* Threshold - Baseline */}
                            <TableCell className="text-right font-mono text-xs font-medium py-2 bg-blue-50 dark:bg-blue-950/30 text-blue-700 dark:text-blue-300">
                              {baselineMetric ? Number(baselineMetric.scalabilityThreshold).toFixed(2) : 'N/A'}
                            </TableCell>
                            {/* Status - Target */}
                            <TableCell className="text-center py-2 bg-purple-50 dark:bg-purple-950/30 border-r">
                              {targetMetric?.passScalabilityThreshold !== undefined ? (
                                <Badge variant={targetMetric.passScalabilityThreshold ? "default" : "destructive"} className={`text-xs ${targetMetric.passScalabilityThreshold ? 'bg-green-600' : ''}`}>
                                  {targetMetric.passScalabilityThreshold ? 'Pass' : 'Fail'}
                                </Badge>
                              ) : 'N/A'}
                            </TableCell>

                            {/* Share - Target only */}
                            <TableCell className="text-center py-2 bg-purple-50 dark:bg-purple-950/30">
                              {targetMetric?.scalabilityShare !== undefined ? (
                                <Badge variant="outline" className="text-xs font-mono">
                                  {targetMetric.scalabilityShare.toFixed(4)}
                                </Badge>
                              ) : 'N/A'}
                            </TableCell>
                          </TableRow>
                        )
                      })}
                    </TableBody>
                  </Table>
                </div>
              </div>
            )
          })()}

          {/* K6 Output Section */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {/* Baseline K6 Output */}
            {data.baselineTestCase?.k6Output && (
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
                      Hide Baseline k6 Output
                    </>
                  ) : (
                    <>
                      <ChevronDown className="h-3 w-3 mr-1" />
                      Show Baseline k6 Output
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

            {/* Target K6 Output */}
            {data.targetTestCases && data.targetTestCases.length > 0 && (() => {
              const testCase = data.targetTestCases.find((tc: TargetTestCaseDto) => tc.load.toString() === selectedLoad)
              if (!testCase?.k6Output) return null

              const testCaseIndex = data.targetTestCases.findIndex((tc: TargetTestCaseDto) => tc.id === testCase.id)

              return (
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
                        Hide Target k6 Output
                      </>
                    ) : (
                      <>
                        <ChevronDown className="h-3 w-3 mr-1" />
                        Show Target k6 Output
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
              )
            })()}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
