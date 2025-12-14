import { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { useBenchmark, useUpdateBenchmark, useValidateOperationalSetting } from '@/hooks/useBenchmarks'
import { useGenerateBehaviorModels } from '@/api/generated/benchmark-controller/benchmark-controller'
import { useToast } from '@/components/ui/use-toast'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Loader2,
  ArrowLeft,
  FileCode,
  Server,
  Activity,
  Users,
  X,
  Play,
  CheckCircle2,
  XCircle,
  ShieldCheck,
  ChevronDown,
  ChevronUp,
  History,
  Database,
  Container,
  Clock,
  PlayCircle,
} from 'lucide-react'
import {useCreateBenchmarkRun, useListBenchmarkRuns} from "@/api/generated/benchmark-run-controller/benchmark-run-controller.ts";
import { format } from 'date-fns'

// Schema for editing - only name and description are editable
const editBenchmarkSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().optional(),
})

type EditBenchmarkFormData = z.infer<typeof editBenchmarkSchema>

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`
}

const getRunStateColor = (state: string) => {
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

const getRunStateClassName = (state: string) => {
  return state === 'COMPLETED' ? 'bg-green-600 hover:bg-green-700' : ''
}

const getRunStateIcon = (state: string) => {
  switch (state) {
    case 'COMPLETED':
      return <CheckCircle2 className="h-3.5 w-3.5" />
    case 'FAILED':
      return <XCircle className="h-3.5 w-3.5" />
    case 'PENDING':
      return <Clock className="h-3.5 w-3.5" />
    case 'RUNNING':
      return <PlayCircle className="h-3.5 w-3.5" />
    default:
      return <History className="h-3.5 w-3.5" />
  }
}

export function BenchmarkDetailPage() {
  const { benchmarkId } = useParams<{ benchmarkId: string }>()
  const navigate = useNavigate()
  const { data, isLoading, error, refetch } = useBenchmark(benchmarkId!)
  const { toast } = useToast()
  const { user, authRequired } = useAuth()
  const updateBenchmark = useUpdateBenchmark()
  const generateBehaviorModels = useGenerateBehaviorModels()
  const runBenchmark = useCreateBenchmarkRun()
  const validateConfig = useValidateOperationalSetting()

  const [isEditing, setIsEditing] = useState(false)
  const [expandedModels, setExpandedModels] = useState<Set<string>>(new Set())
  const [expandedK6Outputs, setExpandedK6Outputs] = useState<Set<string>>(new Set())
  const [showRunModal, setShowRunModal] = useState(false)
  const [testDuration, setTestDuration] = useState('')
  const [showRecentRuns, setShowRecentRuns] = useState(true)

  // Fetch recent runs
  const { data: recentRunsData } = useListBenchmarkRuns(
    { benchmarkId: benchmarkId!, page: 0, size: 5 },
    {
      query: {
        enabled: !!benchmarkId,
        refetchOnWindowFocus: false,
      }
    }
  )

  // Helper to check authentication and redirect to login if needed
  const requireAuth = (): boolean => {
    if (authRequired && !user) {
      sessionStorage.setItem('redirectAfterLogin', window.location.pathname)
      navigate('/login')
      return false
    }
    return true
  }

  // Poll for validation results when any validation is PENDING or RUNNING
  useEffect(() => {
    if (!data) return

    const hasPendingValidation = data.systemsUnderTest?.some(
      (sut) => sut.benchmarkSutValidationRun?.status === 'PENDING' || sut.benchmarkSutValidationRun?.status === 'RUNNING'
    )

    if (hasPendingValidation) {
      const pollInterval = setInterval(() => {
        void refetch()
      }, 3000) // Poll every 3 seconds

      return () => clearInterval(pollInterval)
    }
  }, [data, refetch])

  const form = useForm<EditBenchmarkFormData>({
    resolver: zodResolver(editBenchmarkSchema),
    defaultValues: {
      name: '',
      description: '',
    },
  })

  // Populate form when entering edit mode
  const handleStartEdit = () => {
    if (!requireAuth()) return
    if (!data) return

    form.reset({
      name: data.name,
      description: data.description || '',
    })

    setIsEditing(true)
  }

  const handleCancelEdit = () => {
    setIsEditing(false)
    form.reset()
  }

  const onSubmit = async (formData: EditBenchmarkFormData) => {
    if (!data) return

    try {
      await updateBenchmark.mutateAsync({
        benchmarkId: benchmarkId!,
        data: {
          name: formData.name,
          description: formData.description || undefined,
        },
      })

      toast({
        title: 'Benchmark updated',
        description: 'Your benchmark has been updated successfully',
      })

      setIsEditing(false)
      void refetch()
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to update benchmark',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const handleGenerateBehaviorModels = async () => {
    if (!requireAuth()) return
    try {
      await generateBehaviorModels.mutateAsync({ benchmarkId: benchmarkId! })
      toast({
        title: 'Behavior models generated',
        description: 'Behavior models have been generated successfully',
      })
      void refetch()
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to generate behavior models',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const handleRunBenchmark = () => {
    if (!requireAuth()) return
    setShowRunModal(true)
  }

  const confirmRunBenchmark = async () => {
    if (!requireAuth()) return
    const duration = testDuration.trim() || '1m'

    try {
      const result = await runBenchmark.mutateAsync({
        data: { benchmarkId: benchmarkId!, testDuration: duration },
      })
      toast({
        title: 'Benchmark started',
        description: `Benchmark run ${result.id} has been started${duration ? ` (duration: ${duration})` : ''}`,
        className: 'border-green-500 bg-green-50 text-green-900',
      })
      setShowRunModal(false)
      setTestDuration('')
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to run benchmark',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const handleValidateConfig = async (systemUnderTestId: string) => {
    if (!requireAuth()) return
    try {
      await validateConfig.mutateAsync({ benchmarkId: benchmarkId!, systemUnderTestId })
      toast({
        title: 'Validation started',
        description: 'Configuration validation is running...',
      })
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to start validation',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const toggleModelExpansion = (modelId: string) => {
    setExpandedModels((prev) => {
      const next = new Set(prev)
      if (next.has(modelId)) {
        next.delete(modelId)
      } else {
        next.add(modelId)
      }
      return next
    })
  }

  const toggleK6Output = (systemId: string) => {
    setExpandedK6Outputs((prev) => {
      const next = new Set(prev)
      if (next.has(systemId)) {
        next.delete(systemId)
      } else {
        next.add(systemId)
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
          Error loading benchmark: {(error as Error)?.message || 'Unknown error'}
        </p>
      </div>
    )
  }

  if (isEditing) {
    return (
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-4">
            <Button variant="outline" size="icon" onClick={handleCancelEdit}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-bold">Edit Benchmark</h1>
              <p className="text-muted-foreground mt-1">Update benchmark configuration</p>
            </div>
          </div>
        </div>

        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
          {/* Basic Info */}
          <Card>
            <CardHeader>
              <CardTitle>Basic Information</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name">Name *</Label>
                <Input id="name" {...form.register('name')} />
                {form.formState.errors.name && (
                  <p className="text-sm text-destructive">{form.formState.errors.name.message}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Textarea id="description" {...form.register('description')} rows={3} />
              </div>
            </CardContent>
          </Card>

          {/* Action Buttons */}
          <div className="flex gap-4">
            <Button type="submit" disabled={updateBenchmark.isPending}>
              {updateBenchmark.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              Save Changes
            </Button>
            <Button type="button" variant="outline" onClick={handleCancelEdit} disabled={updateBenchmark.isPending}>
              <X className="h-4 w-4 mr-2" />
              Cancel
            </Button>
          </div>
        </form>
      </div>
    )
  }

  // View mode
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-4">
          <Link to="/benchmarks">
            <Button variant="outline" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <h1 className="text-2xl font-bold">{data.name}</h1>
            {data.description && <p className="text-muted-foreground mt-1">{data.description}</p>}
          </div>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={handleStartEdit}>
            Edit
          </Button>
          <Link to={`/benchmarks/${benchmarkId}/runs`}>
            <Button variant="default" className="bg-blue-600 hover:bg-blue-700">
              <History className="h-4 w-4 mr-2" />
              View Runs
            </Button>
          </Link>
          <Button onClick={handleRunBenchmark} disabled={runBenchmark.isPending}>
            {runBenchmark.isPending ? (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            ) : (
              <Play className="h-4 w-4 mr-2" />
            )}
            Run Benchmark
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6">
        {/* Recent Runs */}
        {recentRunsData && recentRunsData.totalElements > 0 && (
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <History className="h-5 w-5 text-primary" />
                  <CardTitle>Recent Runs</CardTitle>
                  <Badge variant="secondary">{recentRunsData.totalElements} total</Badge>
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setShowRecentRuns(!showRecentRuns)}
                  >
                    {showRecentRuns ? (
                      <>
                        <ChevronUp className="h-4 w-4 mr-1" />
                        Hide
                      </>
                    ) : (
                      <>
                        <ChevronDown className="h-4 w-4 mr-1" />
                        Show
                      </>
                    )}
                  </Button>
                  <Link to={`/benchmarks/${benchmarkId}/runs`}>
                    <Button variant="outline" size="sm">
                      View All Runs
                    </Button>
                  </Link>
                </div>
              </div>
            </CardHeader>
            {showRecentRuns && (
              <CardContent>
                <div className="space-y-3">
                  {recentRunsData.items.map((run) => (
                    <Link
                      key={run.id}
                      to={`/benchmarks/${benchmarkId}/runs/${run.id}`}
                      className="block"
                    >
                      <div className="p-4 rounded-lg border bg-card hover:bg-accent transition-colors">
                        <div className="flex items-start justify-between">
                          <div className="space-y-2 flex-1">
                            <div className="flex items-center gap-2">
                              <span className="font-mono text-sm">{run.id}</span>
                              <Badge variant={getRunStateColor(run.status)} className={`flex items-center gap-1 ${getRunStateClassName(run.status)}`}>
                                {getRunStateIcon(run.status)}
                                {run.status}
                              </Badge>
                            </div>
                            <div className="flex items-center gap-4 text-sm text-muted-foreground">
                              <div className="flex items-center gap-1">
                                <Clock className="h-3.5 w-3.5" />
                                <span>{format(new Date(run.createdAt), 'PPp')}</span>
                              </div>
                              {run.testDuration && (
                                <div className="flex items-center gap-1">
                                  <PlayCircle className="h-3.5 w-3.5" />
                                  <span>Duration: {run.testDuration}</span>
                                </div>
                              )}
                            </div>
                          </div>
                          <Button variant="ghost" size="sm">
                            View Details →
                          </Button>
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              </CardContent>
            )}
          </Card>
        )}

        {/* Operational Setting */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Activity className="h-5 w-5 text-primary" />
                <CardTitle>Operational Setting</CardTitle>
              </div>
              <Button
                variant="default"
                size="sm"
                onClick={handleGenerateBehaviorModels}
                disabled={generateBehaviorModels.isPending}
              >
                {generateBehaviorModels.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Generating...
                  </>
                ) : (
                  <>
                    <Activity className="h-4 w-4 mr-2" />
                    {data.operationalSetting.usageProfile.length > 0 ? 'Regenerate Behavior Models' : 'Generate Behavior Models'}
                  </>
                )}
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* OpenAPI File */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <FileCode className="h-4 w-4" />
                  OpenAPI File
                </div>
                <p className="font-medium">{data.operationalSetting.openApiFile.filename}</p>
              </div>
            </div>

            {/* Behavior Models */}
            {data.operationalSetting.usageProfile.length > 0 && (
              <div className="space-y-2 pt-4 border-t">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Activity className="h-4 w-4" />
                    <span className="text-sm font-medium">Behavior Models</span>
                  </div>
                  <Badge variant="secondary">{data.operationalSetting.usageProfile.length} models</Badge>
                </div>
                <div className="space-y-2">
                  {data.operationalSetting.usageProfile.map((model: any) => {
                    const isExpanded = expandedModels.has(model.id)
                    const frequency = model.frequency ?? model.usageProfile ?? 0
                    const steps = model.steps || []
                    return (
                      <div key={model.id} className="rounded-lg bg-muted/30 p-3 space-y-2">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2 min-w-0 flex-1">
                            <span className="font-medium text-sm">{model.id}</span>
                            <span className="text-xs text-muted-foreground">•</span>
                            <span className="text-xs text-muted-foreground">{model.actor}</span>
                            <span className="text-xs text-muted-foreground">•</span>
                            <span className="text-xs text-muted-foreground">{steps.length} steps</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <Badge variant="outline" className="text-xs">{(frequency * 100).toFixed(0)}%</Badge>
                            <button
                              onClick={() => toggleModelExpansion(model.id)}
                              className="flex items-center gap-1 text-xs text-primary hover:underline"
                            >
                              {isExpanded ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
                            </button>
                          </div>
                        </div>

                        {isExpanded && (
                          <div className="space-y-1 pt-2 border-t">
                            {steps.map((step: any, stepIdx: number) => {
                              const hasParams = step.params && Object.keys(step.params).length > 0
                              let pathWithParams = step.path
                              if (hasParams) {
                                const queryString = Object.entries(step.params)
                                  .map(([key, value]) => `${key}=${value}`)
                                  .join('&')
                                pathWithParams = `${step.path}?${queryString}`
                              }
                              return (
                                <div key={stepIdx} className="rounded bg-background/50 border px-2 py-1.5">
                                  <div className="grid grid-cols-[minmax(150px,200px)_1fr_minmax(80px,120px)] gap-2 items-center text-sm">
                                    <span className="text-xs text-muted-foreground truncate" title={step.operationId}>{step.operationId}</span>
                                    <div className="flex items-center gap-2 min-w-0">
                                      <Badge variant="outline" className="font-mono text-xs flex-shrink-0">{step.method}</Badge>
                                      <span className="font-mono text-xs truncate" title={pathWithParams}>{pathWithParams}</span>
                                    </div>
                                    <div className="flex items-center justify-end gap-1 whitespace-nowrap">
                                      {(step.waitMsFrom > 0 || step.waitMsTo > 0) ? (
                                        <>
                                          <Clock className="h-3 w-3 text-muted-foreground flex-shrink-0" />
                                          <span className="text-xs font-medium">
                                            {step.waitMsFrom === step.waitMsTo ? `${step.waitMsFrom / 1000}s` : `${step.waitMsFrom/1000}-${step.waitMsTo/1000}s`}
                                          </span>
                                        </>
                                      ) : (
                                        <span className="text-xs text-muted-foreground">—</span>
                                      )}
                                    </div>
                                  </div>
                                </div>
                              )
                            })}
                          </div>
                        )}
                      </div>
                    )
                  })}
                </div>
              </div>
            )}

            {/* Operational Profile */}
            {data.operationalSetting.operationalProfile && Object.keys(data.operationalSetting.operationalProfile).length > 0 && (
              <div className="space-y-2 pt-4 border-t">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Users className="h-5 w-5" />
                    <span className="text-sm font-medium">Operational Profile</span>
                  </div>
                  <Badge variant="secondary">{Object.keys(data.operationalSetting.operationalProfile).length} load levels</Badge>
                </div>
                <div className="space-y-3">
                  {Object.entries(data.operationalSetting.operationalProfile).map(([load, frequency]) => (
                    <div key={load} className="flex items-center gap-4 p-3 rounded-lg bg-muted/50">
                      <div className="flex-1">
                        <div className="flex items-baseline gap-2">
                          <span className="text-2xl font-bold">{load}</span>
                          <span className="text-sm text-muted-foreground">users</span>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="h-2 w-32 bg-background rounded-full overflow-hidden">
                          <div
                            className="h-full bg-primary"
                            style={{ width: `${Number(frequency) * 100}%` }}
                          />
                        </div>
                        <span className="text-sm font-medium min-w-[3rem] text-right">
                          {(Number(frequency) * 100).toFixed(1)}%
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Systems Under Test */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Server className="h-5 w-5 text-primary" />
                <CardTitle>Systems Under Test</CardTitle>
              </div>
              <Badge variant="secondary">{data.systemsUnderTest.length}</Badge>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {data.systemsUnderTest.map((sut, index) => (
              <div key={sut.id}>
                {index > 0 && <div className="border-t my-4" />}
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    {sut.isBaseline ? (
                      <Badge variant="default" className="bg-blue-600">Baseline</Badge>
                    ) : (
                      <Badge variant="default" className="bg-purple-600">Target</Badge>
                    )}
                    <Link to={`/systems-under-test/${sut.id}`} className="font-semibold hover:underline">
                      {sut.name}
                    </Link>
                  </div>

                  {sut.description && (
                    <p className="text-sm text-muted-foreground">{sut.description}</p>
                  )}

                  <div className="grid grid-cols-2 gap-3 text-sm">
                    {/* Docker Config */}
                    <div className="space-y-1">
                      <div className="flex items-center gap-1.5 text-muted-foreground">
                        <Container className="h-3.5 w-3.5" />
                        <span className="font-medium">Docker Config</span>
                      </div>
                      <div className="text-xs space-y-0.5 pl-5">
                        <div><span className="text-muted-foreground">Port:</span> <span className="font-mono">{sut.dockerConfig.appPort}</span></div>
                        <div><span className="text-muted-foreground">Health:</span> <span className="font-mono">{sut.dockerConfig.healthCheckPath}</span></div>
                        <div><span className="text-muted-foreground">Timeout:</span> <span className="font-mono">{sut.dockerConfig.startupTimeoutSeconds}s</span></div>
                      </div>
                    </div>

                    {/* Database Config */}
                    {sut.databaseSeedConfigs.length > 0 && (
                      <div className="space-y-1">
                        <div className="flex items-center gap-1.5 text-muted-foreground">
                          <Database className="h-3.5 w-3.5" />
                          <span className="font-medium">Database ({sut.databaseSeedConfigs.length})</span>
                        </div>
                        <div className="text-xs pl-5">
                          {sut.databaseSeedConfigs.map((dbConfig: any, idx: number) => (
                            <div key={idx} className="space-y-0.5">
                              <div className="font-medium">{dbConfig.dbName}</div>
                              <div><span className="text-muted-foreground">Port:</span> <span className="font-mono">{dbConfig.dbPort}</span></div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Validation */}
                  {(() => {
                    const validationRun = sut.benchmarkSutValidationRun
                    const status = validationRun?.status
                    const isRunning = status === 'RUNNING' || status === 'PENDING'
                    const isCompleted = status === 'COMPLETED'
                    const isFailed = status === 'FAILED'

                    return (
                      <div className="flex flex-col gap-2">
                        <Button
                          variant={isCompleted ? 'outline' : isFailed ? 'destructive' : 'outline'}
                          size="sm"
                          onClick={() => handleValidateConfig(sut.id)}
                          disabled={isRunning}
                          className={isCompleted ? 'border-green-600 text-green-600 hover:bg-green-50 w-fit' : 'w-fit'}
                        >
                          {isRunning ? (
                            <>
                              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                              Validating...
                            </>
                          ) : isCompleted ? (
                            <>
                              <CheckCircle2 className="h-4 w-4 mr-2 text-green-600" />
                              Re-validate
                            </>
                          ) : isFailed ? (
                            <>
                              <XCircle className="h-4 w-4 mr-2" />
                              Re-validate
                            </>
                          ) : (
                            <>
                              <ShieldCheck className="h-4 w-4 mr-2" />
                              Validate
                            </>
                          )}
                        </Button>
                        {isFailed && validationRun?.errorMessage && (
                          <div className="p-2 rounded-md bg-destructive/10 border border-destructive/20">
                            <p className="text-xs text-destructive">{validationRun.errorMessage}</p>
                          </div>
                        )}
                        {(isCompleted || isFailed) && validationRun?.k6Output && (
                          <>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => toggleK6Output(sut.id)}
                              className="text-xs w-fit"
                            >
                              {expandedK6Outputs.has(sut.id) ? (
                                <>
                                  <ChevronUp className="h-3 w-3 mr-1" />
                                  Hide Output
                                </>
                              ) : (
                                <>
                                  <ChevronDown className="h-3 w-3 mr-1" />
                                  Show Output
                                </>
                              )}
                            </Button>
                            {expandedK6Outputs.has(sut.id) && (
                              <div className="p-3 rounded-lg bg-background border">
                                <p className="text-xs font-semibold text-muted-foreground mb-2">Validation Output:</p>
                                <pre className="text-xs font-mono whitespace-pre-wrap overflow-x-auto max-h-96 overflow-y-auto p-2 rounded bg-muted/30">
                                  {validationRun.k6Output}
                                </pre>
                              </div>
                            )}
                          </>
                        )}
                      </div>
                    )
                  })()}
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
      {showRunModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <div className="bg-background w-full max-w-md rounded-lg border shadow-lg">
            <div className="p-6 space-y-4">
              <div className="space-y-1">
                <h2 className="text-lg font-semibold">Run Benchmark</h2>
                <p className="text-sm text-muted-foreground">
                  Optional: set test duration. Leave blank to use the default 1m.
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="test-duration">Test Duration</Label>
                <Input
                  id="test-duration"
                  placeholder="1m (default)"
                  value={testDuration}
                  onChange={(e) => setTestDuration(e.target.value)}
                  disabled={runBenchmark.isPending}
                />
                <p className="text-xs text-muted-foreground">Sent to the backend as a request parameter.</p>
              </div>

              <div className="flex justify-end gap-2">
                <Button variant="outline" type="button" onClick={() => setShowRunModal(false)} disabled={runBenchmark.isPending}>
                  Cancel
                </Button>
                <Button type="button" onClick={confirmRunBenchmark} disabled={runBenchmark.isPending}>
                  {runBenchmark.isPending ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      Starting...
                    </>
                  ) : (
                    'Start Run'
                  )}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
