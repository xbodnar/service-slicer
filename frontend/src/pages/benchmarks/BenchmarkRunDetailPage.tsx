import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useGetBenchmarkRun } from '@/api/generated/benchmarks-controller/benchmarks-controller'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { ArrowLeft, Loader2, CheckCircle, XCircle, Clock, PlayCircle, Activity, ChevronDown, ChevronUp } from 'lucide-react'
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
              <h1 className="text-2xl font-bold">Benchmark Run</h1>
              <Badge variant={getStateColor(data.state)} className={`flex items-center gap-1 ${getStateClassName(data.state)}`}>
                {getStateIcon(data.state)}
                {data.state}
              </Badge>
            </div>
            <p className="text-muted-foreground mt-1 font-mono text-sm">{data.benchmarkRunId}</p>
          </div>
        </div>
      </div>

      {/* Run Info */}
      <Card>
        <CardHeader>
          <CardTitle>Run Information</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-muted-foreground">Created</p>
              <p className="font-medium">{format(new Date(data.createdAt), 'PPpp')}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Last Updated</p>
              <p className="font-medium">{format(new Date(data.updatedAt), 'PPpp')}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Systems Tested</p>
              <p className="font-medium">{data.architectureTestSuites.length}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Benchmark ID</p>
              <p className="font-mono text-xs">{data.benchmarkId}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Baseline Test Case */}
      {data.baselineTestCase && (
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <CardTitle>Baseline Test Case</CardTitle>
              <Badge variant="default" className="bg-blue-600">Baseline</Badge>
            </div>
          </CardHeader>
          <CardContent>
            <div className="p-4 rounded-lg border bg-muted/30 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-medium">{data.baselineTestCase.load} users</span>
                  <Badge variant={getStateColor(data.baselineTestCase.status)} className={`flex items-center gap-1 ${getStateClassName(data.baselineTestCase.status)}`}>
                    {getStateIcon(data.baselineTestCase.status)}
                    {data.baselineTestCase.status}
                  </Badge>
                </div>
                <p className="text-xs font-mono text-muted-foreground">{data.baselineTestCase.baselineSutId}</p>
              </div>

              {data.baselineTestCase.startTimestamp && data.baselineTestCase.endTimestamp && (
                <div className="text-xs text-muted-foreground">
                  {format(new Date(data.baselineTestCase.startTimestamp), 'PPpp')} - {format(new Date(data.baselineTestCase.endTimestamp), 'pp')}
                </div>
              )}

              {/* Operation Metrics */}
              {data.baselineTestCase.operationMeasurements && Object.keys(data.baselineTestCase.operationMeasurements).length > 0 ? (
                <div className="space-y-2">
                  <p className="text-xs font-semibold">Operation Metrics:</p>
                  {Object.entries(data.baselineTestCase.operationMeasurements).map(([operationId, metric]: [string, any]) => (
                    <div key={operationId} className="p-2 rounded bg-muted/30 space-y-1">
                      <p className="text-xs font-mono font-medium">{operationId}</p>
                      <div className="grid grid-cols-2 gap-2 text-xs">
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Total Requests</span>
                          <span className="font-mono">{metric.totalRequests}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Failed</span>
                          <span className="font-mono">{metric.failedRequests}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Mean Response</span>
                          <span className="font-mono">{Number(metric.meanResponseTimeMs).toFixed(2)}ms</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">P95</span>
                          <span className="font-mono">{Number(metric.p95DurationMs).toFixed(2)}ms</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-muted-foreground">No operation metrics available</p>
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
            </div>
          </CardContent>
        </Card>
      )}

      {/* Architecture Test Suites Results */}
      <Card>
        <CardHeader>
          <CardTitle>System Under Test Results</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {data.architectureTestSuites.length === 0 ? (
            <p className="text-center text-muted-foreground py-8">No test results available</p>
          ) : (
            data.architectureTestSuites.map((testSuite, index) => (
              <div key={index} className="p-4 rounded-lg border bg-muted/30">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <h4 className="font-semibold">System {index + 1}</h4>
                    <Badge variant={getStateColor(testSuite.status)} className={`flex items-center gap-1 ${getStateClassName(testSuite.status)}`}>
                      {getStateIcon(testSuite.status)}
                      {testSuite.status}
                    </Badge>
                  </div>
                  <p className="text-xs font-mono text-muted-foreground">{testSuite.targetSutId}</p>
                </div>

                {/* Target Test Cases */}
                {testSuite.targetTestCases.length > 0 ? (
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-muted-foreground">Load Test Results</p>
                    <div className="space-y-3">
                      {testSuite.targetTestCases.map((testCase, testCaseIndex) => (
                        <div key={testCaseIndex} className="p-3 rounded-md bg-background border">
                          <div className="flex items-center justify-between mb-3">
                            <span className="text-sm font-medium">{testCase.load} users</span>
                            <Badge variant={getStateColor(testCase.status)} className={`text-xs ${getStateClassName(testCase.status)}`}>
                              {testCase.status}
                            </Badge>
                          </div>
                          {testCase.startTimestamp && testCase.endTimestamp && (
                            <div className="text-xs text-muted-foreground mb-2">
                              {format(new Date(testCase.startTimestamp), 'PPpp')} - {format(new Date(testCase.endTimestamp), 'pp')}
                            </div>
                          )}

                          {/* Operation Metrics */}
                          {testCase.operationMeasurements && Object.keys(testCase.operationMeasurements).length > 0 ? (
                            <div className="space-y-2">
                              <p className="text-xs font-semibold">Operation Metrics:</p>
                              {Object.entries(testCase.operationMeasurements).map(([operationId, metric]: [string, any]) => (
                                <div key={operationId} className="p-2 rounded bg-muted/30 space-y-1">
                                  <p className="text-xs font-mono font-medium">{operationId}</p>
                                  <div className="grid grid-cols-2 gap-2 text-xs">
                                    <div className="flex justify-between">
                                      <span className="text-muted-foreground">Total Requests</span>
                                      <span className="font-mono">{metric.totalRequests}</span>
                                    </div>
                                    <div className="flex justify-between">
                                      <span className="text-muted-foreground">Failed</span>
                                      <span className="font-mono">{metric.failedRequests}</span>
                                    </div>
                                    <div className="flex justify-between">
                                      <span className="text-muted-foreground">Mean Response</span>
                                      <span className="font-mono">{Number(metric.meanResponseTimeMs).toFixed(2)}ms</span>
                                    </div>
                                    <div className="flex justify-between">
                                      <span className="text-muted-foreground">P95</span>
                                      <span className="font-mono">{Number(metric.p95DurationMs).toFixed(2)}ms</span>
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <p className="text-xs text-muted-foreground">No operation metrics available</p>
                          )}

                          {/* K6 Output */}
                          {testCase.k6Output && (
                            <div className="space-y-2 mt-3">
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => toggleK6Output(`${testSuite.targetSutId}-${testCaseIndex}`)}
                                className="text-xs"
                              >
                                {expandedK6Outputs.has(`${testSuite.targetSutId}-${testCaseIndex}`) ? (
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
                              {expandedK6Outputs.has(`${testSuite.targetSutId}-${testCaseIndex}`) && (
                                <div className="p-3 rounded-lg bg-background border">
                                  <pre className="text-xs font-mono whitespace-pre-wrap overflow-x-auto max-h-96 overflow-y-auto p-2 rounded bg-muted/30">
                                    {testCase.k6Output}
                                  </pre>
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">No test cases available</p>
                )}
              </div>
            ))
          )}
        </CardContent>
      </Card>
    </div>
  )
}
