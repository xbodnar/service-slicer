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
} from 'lucide-react'
import {useCreateBenchmarkRun} from "@/api/generated/benchmark-run-controller/benchmark-run-controller.ts";

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
          <Link to={`/benchmarks/${benchmarkId}/runs`}>
            <Button variant="outline">
              <History className="h-4 w-4 mr-2" />
              View Runs
            </Button>
          </Link>
          <Button variant="outline" onClick={handleStartEdit}>
            Edit
          </Button>
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
          <CardContent className="space-y-5">
            {/* OpenAPI File */}
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                <FileCode className="h-4 w-4" />
                <span>OpenAPI Specification</span>
              </div>
              <div className="flex items-center gap-2 pl-6">
                <span className="font-medium">{data.operationalSetting.openApiFile.filename}</span>
                <Badge variant="secondary" className="text-xs">
                  {formatFileSize(data.operationalSetting.openApiFile.fileSize)}
                </Badge>
              </div>
            </div>

            {/* Behavior Models */}
            {data.operationalSetting.usageProfile.length > 0 && (
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm font-medium text-muted-foreground">Behavior Models</span>
                  <Badge variant="outline">{data.operationalSetting.usageProfile.length}</Badge>
                </div>
                <div className="space-y-2 pl-6">
                  {data.operationalSetting.usageProfile.map((model: any) => {
                    const isExpanded = expandedModels.has(model.id)
                    const frequency = model.frequency ?? model.usageProfile ?? 0
                    const steps = model.steps || []
                    return (
                      <div key={model.id} className="p-3 rounded-lg bg-muted/50 space-y-2">
                        <div className="flex items-center gap-2">
                          <span className="font-medium text-sm">{model.id}</span>
                          <span className="text-xs text-muted-foreground">-</span>
                          <span className="text-sm text-muted-foreground">{model.actor}</span>
                          <Badge variant="secondary" className="ml-auto">
                            {(frequency * 100).toFixed(0)}%
                          </Badge>
                        </div>

                        {!isExpanded && (
                          <p className="text-xs text-muted-foreground">
                            {steps.map((step: any) => `${step.method} ${step.path}`).join(' -> ')}
                          </p>
                        )}

                        <button
                          onClick={() => toggleModelExpansion(model.id)}
                          className="flex items-center gap-1 text-xs text-primary hover:underline"
                        >
                          {isExpanded ? (
                            <>
                              <ChevronUp className="h-3 w-3" />
                              Show less
                            </>
                          ) : (
                            <>
                              <ChevronDown className="h-3 w-3" />
                              Show more
                            </>
                          )}
                        </button>

                        {isExpanded && (
                          <div className="space-y-3 pt-2 border-t">
                            {/* Steps */}
                            <div className="space-y-2">
                              <p className="text-xs font-semibold text-muted-foreground">API Request Steps:</p>
                              {model.steps.map((step: any, stepIndex: number) => (
                                <div key={stepIndex} className="p-2 rounded-md bg-background/50 border space-y-2">
                                  <div className="flex items-center gap-2">
                                    <Badge variant="outline" className="text-xs">
                                      Step {stepIndex + 1}
                                    </Badge>
                                    <Badge variant="secondary" className="text-xs">
                                      {step.method}
                                    </Badge>
                                    <span className="font-mono text-xs">{step.path}</span>
                                  </div>

                                  {/* Headers */}
                                  {step.headers && Object.keys(step.headers).length > 0 && (
                                    <div className="text-xs">
                                      <p className="font-semibold text-muted-foreground mb-1">Headers:</p>
                                      <div className="space-y-1 pl-2">
                                        {Object.entries(step.headers).map(([key, value]: [string, any]) => (
                                          <div key={key} className="flex gap-2">
                                            <span className="text-muted-foreground">{key}:</span>
                                            <span className="font-mono">{String(value)}</span>
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  )}

                                  {/* Query Parameters */}
                                  {step.params && Object.keys(step.params).length > 0 && (
                                    <div className="text-xs">
                                      <p className="font-semibold text-muted-foreground mb-1">Query Parameters:</p>
                                      <div className="space-y-1 pl-2">
                                        {Object.entries(step.params).map(([key, value]: [string, any]) => (
                                          <div key={key} className="flex gap-2">
                                            <span className="text-muted-foreground">{key}:</span>
                                            <span className="font-mono">{String(value)}</span>
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  )}

                                  {/* Body */}
                                  {step.body && Object.keys(step.body).length > 0 && (
                                    <div className="text-xs">
                                      <p className="font-semibold text-muted-foreground mb-1">Body:</p>
                                      <pre className="pl-2 font-mono text-xs bg-muted/30 p-2 rounded overflow-x-auto">
                                        {JSON.stringify(step.body, null, 2)}
                                      </pre>
                                    </div>
                                  )}

                                  {/* Save Fields */}
                                  {step.save && Object.keys(step.save).length > 0 && (
                                    <div className="text-xs">
                                      <p className="font-semibold text-muted-foreground mb-1">Save Fields:</p>
                                      <div className="space-y-1 pl-2">
                                        {Object.entries(step.save).map(([key, value]: [string, any]) => (
                                          <div key={key} className="flex gap-2">
                                            <span className="text-muted-foreground">{key}:</span>
                                            <span className="font-mono">{String(value)}</span>
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  )}
                                </div>
                              ))}
                            </div>
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
              <div className="space-y-2">
                <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                  <Activity className="h-4 w-4" />
                  <span>Operational Profile</span>
                </div>
                <div className="pl-6 text-sm">
                  {Object.entries(data.operationalSetting.operationalProfile).map(([load, frequency], i) => (
                    <span key={load}>
                      {load} users ({(Number(frequency) * 100).toFixed(0)}%)
                      {i < Object.keys(data.operationalSetting.operationalProfile).length - 1 && ', '}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Systems Under Test */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Server className="h-5 w-5 text-primary" />
              <CardTitle>Systems Under Test</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-6">
            {data.systemsUnderTest.map((sut, index) => (
              <div key={sut.id}>
                {index > 0 && <div className="border-t mb-6" />}
                <div>
                  <div className="flex items-center gap-2 mb-3">
                    {sut.isBaseline ? (
                      <Badge variant="default" className="bg-blue-600">Baseline</Badge>
                    ) : (
                      <Badge variant="default" className="bg-purple-600">Target</Badge>
                    )}
                    <Link to={`/systems-under-test/${sut.id}`} className="text-lg font-semibold hover:underline">
                      {sut.name}
                    </Link>
                  </div>

                  {sut.description && (
                    <p className="text-sm text-muted-foreground mb-3">{sut.description}</p>
                  )}

                  {/* Docker Configuration */}
                  <div className="mb-3 p-3 rounded-lg bg-muted/30 space-y-2">
                    <div className="flex items-center gap-2 text-sm font-medium">
                      <Container className="h-4 w-4 text-muted-foreground" />
                      <span>Docker Configuration</span>
                    </div>
                    <div className="pl-6 space-y-1 text-sm">
                      <div className="flex items-center gap-2">
                        <FileCode className="h-3 w-3 text-muted-foreground" />
                        <span className="text-muted-foreground">Compose File:</span>
                        <span className="font-medium">{sut.dockerConfig.composeFile.filename}</span>
                      </div>
                      <div className="grid grid-cols-2 gap-2 pl-5">
                        <div>
                          <span className="text-muted-foreground">App Port:</span>
                          <span className="ml-2 font-mono">{sut.dockerConfig.appPort}</span>
                        </div>
                        <div>
                          <span className="text-muted-foreground">Health Check:</span>
                          <span className="ml-2 font-mono">{sut.dockerConfig.healthCheckPath}</span>
                        </div>
                        <div>
                          <span className="text-muted-foreground">Startup Timeout:</span>
                          <span className="ml-2 font-mono">{sut.dockerConfig.startupTimeoutSeconds}s</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Database Seed Configurations */}
                  {sut.databaseSeedConfigs.length > 0 && (
                    <div className="mb-3 p-3 rounded-lg bg-muted/30 space-y-2">
                      <div className="flex items-center gap-2 text-sm font-medium">
                        <Database className="h-4 w-4 text-muted-foreground" />
                        <span>Database Configurations ({sut.databaseSeedConfigs.length})</span>
                      </div>
                      <div className="pl-6 space-y-3">
                        {sut.databaseSeedConfigs.map((dbConfig: any, idx: number) => (
                          <div key={idx} className="p-2 rounded-md bg-background/50 border text-xs space-y-1">
                            <div className="flex items-center gap-2">
                              <span className="font-semibold">{dbConfig.dbName}</span>
                              <Badge variant="outline" className="text-xs">{dbConfig.dbContainerName}</Badge>
                            </div>
                            <div className="grid grid-cols-2 gap-2 text-xs">
                              <div>
                                <span className="text-muted-foreground">Port:</span>
                                <span className="ml-2 font-mono">{dbConfig.dbPort}</span>
                              </div>
                              <div>
                                <span className="text-muted-foreground">Username:</span>
                                <span className="ml-2 font-mono">{dbConfig.dbUsername}</span>
                              </div>
                            </div>
                            <div className="flex items-center gap-1">
                              <FileCode className="h-3 w-3 text-muted-foreground" />
                              <span className="text-muted-foreground">Seed File:</span>
                              <span className="font-mono">{dbConfig.sqlSeedFile.filename}</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Validation */}
                  <div className="mb-3">
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
