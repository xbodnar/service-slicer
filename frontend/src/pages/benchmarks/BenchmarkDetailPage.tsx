import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { useBenchmark, useUpdateBenchmark } from '@/hooks/useBenchmarks'
import { useGenerateBehaviorModels, useRunBenchmark, useValidateBenchmarkConfig } from '@/api/generated/benchmarks-controller/benchmarks-controller'
import { type UploadedFile } from '@/hooks/useFileUpload'
import { useToast } from '@/components/ui/use-toast'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { FileSelector } from '@/components/ui/file-selector'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  Loader2,
  ArrowLeft,
  FileCode,
  Server,
  Activity,
  Users,
  X,
  Plus,
  Trash2,
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

// Form schema matching API structure
const keyValuePairSchema = z.object({
  key: z.string(),
  value: z.string(),
})

// Schema for editing
const editBenchmarkSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().optional(),
  behaviorModels: z.array(
    z.object({
      id: z.string().min(1, 'ID is required'),
      actor: z.string().min(1, 'Actor name is required'),
      usageProfile: z.coerce.number().min(0).max(1),
      steps: z.array(
        z.object({
          method: z.string().min(1, 'Method is required'),
          path: z.string().min(1, 'Path is required'),
          operationId: z.string().min(1, 'Operation ID is required'),
          headers: z.array(keyValuePairSchema).default([]),
          params: z.array(keyValuePairSchema).default([]),
          body: z.string().default('{}'),
          save: z.array(keyValuePairSchema).default([]),
        })
      ).min(1, 'At least one step is required'),
      thinkFrom: z.coerce.number().min(0),
      thinkTo: z.coerce.number().min(0),
    })
  ).optional(),
  operationalProfile: z.array(
    z.object({
      load: z.coerce.number().min(1, 'Load must be at least 1'),
      frequency: z.coerce.number().min(0).max(1, 'Frequency must be between 0 and 1'),
    })
  ).min(1, 'At least one operational load is required'),
})

type EditBenchmarkFormData = z.infer<typeof editBenchmarkSchema>

interface KeyValuePairListProps {
  name: string
  control: any
  label: string
  keyPlaceholder?: string
  valuePlaceholder?: string
}

function KeyValuePairList({ name, control, label, keyPlaceholder = 'Key', valuePlaceholder = 'Value' }: KeyValuePairListProps) {
  const { fields, append, remove } = useFieldArray({
    control,
    name,
  })

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Label className="text-xs">{label}</Label>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="h-6 px-2"
          onClick={() => append({ key: '', value: '' })}
        >
          <Plus className="h-3 w-3" />
        </Button>
      </div>
      <div className="space-y-1.5">
        {fields.map((field, index) => (
          <div key={field.id} className="flex items-center gap-1.5">
            <Input
              {...control.register(`${name}.${index}.key`)}
              placeholder={keyPlaceholder}
              className="h-7 text-xs flex-1"
            />
            <Input
              {...control.register(`${name}.${index}.value`)}
              placeholder={valuePlaceholder}
              className="h-7 text-xs flex-1"
            />
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-7 w-7 p-0"
              onClick={() => remove(index)}
            >
              <Trash2 className="h-3 w-3" />
            </Button>
          </div>
        ))}
        {fields.length === 0 && (
          <p className="text-xs text-muted-foreground italic">No {label.toLowerCase()} defined</p>
        )}
      </div>
    </div>
  )
}

interface BehaviorModelStepsProps {
  behaviorIndex: number
  control: any
  register: any
  errors: any
}

function BehaviorModelSteps({ behaviorIndex, control, register, errors }: BehaviorModelStepsProps) {
  const { fields: stepFields, append: appendStep, remove: removeStep } = useFieldArray({
    control,
    name: `behaviorModels.${behaviorIndex}.steps`,
  })

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <Label>API Request Steps</Label>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => appendStep({ method: 'GET', path: '/', operationId: '', headers: [], params: [], body: '{}', save: [] })}
        >
          <Plus className="h-4 w-4 mr-2" />
          Add Step
        </Button>
      </div>

      {stepFields.map((field: any, stepIndex) => (
        <Card key={field.id} className="p-3 bg-muted/30">
          <div className="space-y-3">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium">Step {stepIndex + 1}</span>
              <Button type="button" variant="ghost" size="sm" onClick={() => removeStep(stepIndex)}>
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label>Method</Label>
                <Select
                  defaultValue={field.method}
                  onValueChange={(value) => {
                    const input = document.getElementById(`edit-behavior-${behaviorIndex}-step-${stepIndex}-method-hidden`) as HTMLInputElement
                    if (input) input.value = value
                  }}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select method" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="GET">GET</SelectItem>
                    <SelectItem value="POST">POST</SelectItem>
                    <SelectItem value="PUT">PUT</SelectItem>
                    <SelectItem value="PATCH">PATCH</SelectItem>
                    <SelectItem value="DELETE">DELETE</SelectItem>
                  </SelectContent>
                </Select>
                <input
                  type="hidden"
                  id={`edit-behavior-${behaviorIndex}-step-${stepIndex}-method-hidden`}
                  {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.method`)}
                />
              </div>

              <div className="space-y-2">
                <Label>Path</Label>
                <Input
                  {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.path`)}
                  placeholder="/api/resource"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label>Operation ID</Label>
              <Input
                {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.operationId`)}
                placeholder="getUser"
              />
              {errors?.behaviorModels?.[behaviorIndex]?.steps?.[stepIndex]?.operationId && (
                <p className="text-sm text-destructive">
                  {errors.behaviorModels[behaviorIndex].steps[stepIndex].operationId.message}
                </p>
              )}
            </div>

            {/* Headers, Query Params, and Save Fields side by side */}
            <div className="grid grid-cols-3 gap-3">
              <KeyValuePairList
                name={`behaviorModels.${behaviorIndex}.steps.${stepIndex}.headers`}
                control={control}
                label="Headers"
                keyPlaceholder="Header name"
                valuePlaceholder="Header value"
              />
              <KeyValuePairList
                name={`behaviorModels.${behaviorIndex}.steps.${stepIndex}.params`}
                control={control}
                label="Query Params"
                keyPlaceholder="Param name"
                valuePlaceholder="Param value"
              />
              <KeyValuePairList
                name={`behaviorModels.${behaviorIndex}.steps.${stepIndex}.save`}
                control={control}
                label="Save Fields"
                keyPlaceholder="Variable name"
                valuePlaceholder="JSONPath ($.id)"
              />
            </div>

            <div className="space-y-2">
              <Label>Body (JSON)</Label>
              <Textarea
                {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.body`)}
                placeholder='{"key": "value"}'
                rows={3}
                className="font-mono text-xs"
              />
              {errors?.behaviorModels?.[behaviorIndex]?.steps?.[stepIndex]?.body && (
                <p className="text-sm text-destructive">
                  {errors.behaviorModels[behaviorIndex].steps[stepIndex].body.message}
                </p>
              )}
            </div>
          </div>
        </Card>
      ))}
    </div>
  )
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`
}

export function BenchmarkDetailPage() {
  const { benchmarkId } = useParams<{ benchmarkId: string }>()
  const { data, isLoading, error, refetch } = useBenchmark(benchmarkId!)
  const { toast } = useToast()
  const updateBenchmark = useUpdateBenchmark()
  const generateBehaviorModels = useGenerateBehaviorModels()
  const runBenchmark = useRunBenchmark()
  const validateConfig = useValidateBenchmarkConfig()

  const [isEditing, setIsEditing] = useState(false)
  const [openApiFile, setOpenApiFile] = useState<UploadedFile | null>(null)
  const [expandedModels, setExpandedModels] = useState<Set<string>>(new Set())
  const [expandedK6Outputs, setExpandedK6Outputs] = useState<Set<string>>(new Set())

  // Poll for validation results when any validation is PENDING
  useEffect(() => {
    if (!data) return

    const hasPendingValidation =
      data.baselineSutValidationResult?.validationState === 'PENDING' ||
      data.targetSutValidationResult?.validationState === 'PENDING'

    if (hasPendingValidation) {
      const pollInterval = setInterval(() => {
        refetch()
      }, 3000) // Poll every 3 seconds

      return () => clearInterval(pollInterval)
    }
  }, [data, refetch])

  const form = useForm<EditBenchmarkFormData>({
    resolver: zodResolver(editBenchmarkSchema),
    defaultValues: {
      name: '',
      description: '',
      behaviorModels: [],
      operationalProfile: [],
    },
  })

  const { fields: behaviorFields, append: appendBehavior, remove: removeBehavior } = useFieldArray({
    control: form.control,
    name: 'behaviorModels',
  })

  const { fields: operationalProfileFields, append: appendOperationalProfile, remove: removeOperationalProfile } = useFieldArray({
    control: form.control,
    name: 'operationalProfile',
  })

  // Helper to convert object to key-value pairs array
  const objectToKeyValuePairs = (obj: Record<string, string> | undefined): { key: string; value: string }[] => {
    if (!obj) return []
    return Object.entries(obj).map(([key, value]) => ({ key, value: String(value) }))
  }

  // Helper to convert key-value pairs array to object
  const keyValuePairsToObject = (pairs: { key: string; value: string }[]): Record<string, string> => {
    return pairs.reduce((acc, { key, value }) => {
      if (key.trim()) {
        acc[key] = value
      }
      return acc
    }, {} as Record<string, string>)
  }

  // Populate form when entering edit mode
  const handleStartEdit = () => {
    if (!data) return

    const behaviorModels = data.config.behaviorModels.map((model: any) => ({
      id: model.id,
      actor: model.actor,
      usageProfile: model.usageProfile,
      steps: model.steps.map((step: any) => ({
        method: step.method,
        path: step.path,
        operationId: step.operationId,
        headers: objectToKeyValuePairs(step.headers),
        params: objectToKeyValuePairs(step.params),
        body: JSON.stringify(step.body || {}),
        save: objectToKeyValuePairs(step.save),
      })),
      thinkFrom: model.thinkFrom,
      thinkTo: model.thinkTo,
    }))

    form.reset({
      name: data.name,
      description: data.description || '',
      behaviorModels,
      operationalProfile: data.config.operationalProfile || [],
    })

    setOpenApiFile(null)
    setIsEditing(true)
  }

  const handleCancelEdit = () => {
    setIsEditing(false)
    setOpenApiFile(null)
    form.reset()
  }

  const handleAddBehaviorModel = () => {
    appendBehavior({
      id: '',
      actor: '',
      usageProfile: 0.5,
      steps: [{ method: 'GET', path: '/', operationId: '', headers: [], params: [], body: '{}', save: [] }],
      thinkFrom: 1000,
      thinkTo: 3000,
    })
  }

  const handleAddOperationalProfile = () => {
    appendOperationalProfile({ load: 25, frequency: 0.2 })
  }

  const onSubmit = async (formData: EditBenchmarkFormData) => {
    if (!data) return

    try {
      // Process behavioral models
      const behaviorModels = (formData.behaviorModels || []).map((model) => {
        const steps = model.steps.map((step) => {
          const headers = keyValuePairsToObject(step.headers || [])
          const params = keyValuePairsToObject(step.params || [])
          const save = keyValuePairsToObject(step.save || [])
          let body
          try {
            body = step.body && step.body.trim() !== '' ? JSON.parse(step.body) : {}
          } catch (e) {
            throw new Error(`Invalid JSON in step "${step.path}": ${e}`)
          }
          return {
            method: step.method,
            path: step.path,
            operationId: step.operationId,
            headers,
            params,
            body,
            save,
          }
        })
        return {
          id: model.id,
          actor: model.actor,
          usageProfile: model.usageProfile,
          steps,
          thinkFrom: model.thinkFrom,
          thinkTo: model.thinkTo,
        }
      })

      // Validate operational profile
      const freqSum = formData.operationalProfile.reduce((sum, p) => sum + Number(p.frequency), 0)
      if (Math.abs(freqSum - 1.0) > 0.001) {
        toast({
          variant: 'destructive',
          title: 'Invalid operational profile',
          description: `Frequencies must sum to 1.0 (current sum: ${freqSum.toFixed(3)})`,
        })
        return
      }

      await updateBenchmark.mutateAsync({
        benchmarkId: benchmarkId!,
        data: {
          name: formData.name,
          description: formData.description || undefined,
          benchmarkConfig: {
            id: data.config.loadTestConfigId,
            openApiFileId: openApiFile?.fileId || data.config.openApiFile.fileId,
            behaviorModels,
            operationalProfile: formData.operationalProfile,
          },
        },
      })

      toast({
        title: 'Benchmark updated',
        description: 'Your benchmark has been updated successfully',
      })

      setIsEditing(false)
      setOpenApiFile(null)
      refetch()
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to update benchmark',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const handleGenerateBehaviorModels = async () => {
    try {
      await generateBehaviorModels.mutateAsync({ benchmarkId: benchmarkId! })
      toast({
        title: 'Behavior models generated',
        description: 'Behavior models have been generated successfully',
      })
      refetch()
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to generate behavior models',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const handleRunBenchmark = async () => {
    try {
      const result = await runBenchmark.mutateAsync({ benchmarkId: benchmarkId! })
      toast({
        title: 'Benchmark started',
        description: `Benchmark run ${result.benchmarkRunId} has been started`,
      })
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to run benchmark',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const handleValidateConfig = async (systemUnderTestId: string) => {
    try {
      await validateConfig.mutateAsync({ benchmarkId: benchmarkId!, systemUnderTestId })
      toast({
        title: 'Validation started',
        description: 'Configuration validation is running...',
      })
      // Refetch to get the updated validation result
      refetch()
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

          {/* Benchmark Configuration */}
          <Card>
            <CardHeader>
              <CardTitle>Benchmark Configuration</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* OpenAPI File */}
              <div className="space-y-2">
                <FileSelector
                  id="edit-openapi-file"
                  label="OpenAPI Specification File (leave empty to keep current)"
                  accept=".json,.yaml,.yml"
                  onFileSelected={setOpenApiFile}
                  selectedFile={openApiFile}
                  mimeTypeFilter="json"
                />
                {!openApiFile && (
                  <div className="flex items-center gap-2 p-3 rounded-lg bg-muted/50 border">
                    <FileCode className="h-4 w-4 text-muted-foreground" />
                    <span className="text-sm">Current: {data.config.openApiFile.filename}</span>
                  </div>
                )}
              </div>

              {/* Behavior Models */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <Label>Behavior Models</Label>
                  <Button type="button" variant="outline" size="sm" onClick={handleAddBehaviorModel}>
                    <Plus className="h-4 w-4 mr-2" />
                    Add Model
                  </Button>
                </div>

                {behaviorFields.map((field, index) => (
                  <Card key={field.id} className="p-4">
                    <div className="space-y-3">
                      <div className="flex items-end gap-2">
                        <div className="flex-1 grid grid-cols-2 gap-3">
                          <div className="space-y-2">
                            <Label>ID</Label>
                            <Input {...form.register(`behaviorModels.${index}.id`)} placeholder="checkout-flow" />
                          </div>
                          <div className="space-y-2">
                            <Label>Actor</Label>
                            <Input {...form.register(`behaviorModels.${index}.actor`)} placeholder="Customer" />
                          </div>
                        </div>
                        <Button type="button" variant="ghost" size="icon" onClick={() => removeBehavior(index)}>
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>

                      <div className="grid grid-cols-3 gap-3">
                        <div className="space-y-2">
                          <Label>Usage Profile</Label>
                          <Input type="number" step="0.01" min="0" max="1" {...form.register(`behaviorModels.${index}.usageProfile`)} />
                        </div>
                        <div className="space-y-2">
                          <Label>Think From (ms)</Label>
                          <Input type="number" {...form.register(`behaviorModels.${index}.thinkFrom`)} />
                        </div>
                        <div className="space-y-2">
                          <Label>Think To (ms)</Label>
                          <Input type="number" {...form.register(`behaviorModels.${index}.thinkTo`)} />
                        </div>
                      </div>

                      <BehaviorModelSteps
                        behaviorIndex={index}
                        control={form.control}
                        register={form.register}
                        errors={form.formState.errors}
                      />
                    </div>
                  </Card>
                ))}
              </div>

              {/* Operational Profile */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <Label>Operational Profile</Label>
                  <Button type="button" variant="outline" size="sm" onClick={handleAddOperationalProfile}>
                    <Plus className="h-4 w-4 mr-2" />
                    Add Load Level
                  </Button>
                </div>

                {operationalProfileFields.map((field, index) => (
                  <div key={field.id} className="flex items-center gap-4 p-3 rounded-lg bg-muted/50">
                    <div className="flex-1 grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label>Load (users)</Label>
                        <Input type="number" {...form.register(`operationalProfile.${index}.load`)} />
                      </div>
                      <div className="space-y-2">
                        <Label>Frequency (0-1)</Label>
                        <Input type="number" step="0.001" min="0" max="1" {...form.register(`operationalProfile.${index}.frequency`)} />
                      </div>
                    </div>
                    {operationalProfileFields.length > 1 && (
                      <Button type="button" variant="ghost" size="icon" onClick={() => removeOperationalProfile(index)}>
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                ))}
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
        {/* Benchmark Configuration */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Activity className="h-5 w-5 text-primary" />
                <CardTitle>Benchmark Configuration</CardTitle>
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
                    {data.config.behaviorModels.length > 0 ? 'Regenerate Behavior Models' : 'Generate Behavior Models'}
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
                <span className="font-medium">{data.config.openApiFile.filename}</span>
                <Badge variant="secondary" className="text-xs">
                  {formatFileSize(data.config.openApiFile.fileSize)}
                </Badge>
              </div>
            </div>

            {/* Behavior Models */}
            {data.config.behaviorModels.length > 0 && (
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm font-medium text-muted-foreground">Behavior Models</span>
                  <Badge variant="outline">{data.config.behaviorModels.length}</Badge>
                </div>
                <div className="space-y-2 pl-6">
                  {data.config.behaviorModels.map((model: any) => {
                    const isExpanded = expandedModels.has(model.id)
                    return (
                      <div key={model.id} className="p-3 rounded-lg bg-muted/50 space-y-2">
                        <div className="flex items-center gap-2">
                          <span className="font-medium text-sm">{model.id}</span>
                          <span className="text-xs text-muted-foreground">-</span>
                          <span className="text-sm text-muted-foreground">{model.actor}</span>
                          <Badge variant="secondary" className="ml-auto">
                            {(model.usageProfile * 100).toFixed(0)}%
                          </Badge>
                        </div>

                        {!isExpanded && (
                          <p className="text-xs text-muted-foreground">
                            {model.steps.map((step: any) => `${step.method} ${step.path}`).join(' -> ')}
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
                            {/* Think Time */}
                            <div className="grid grid-cols-2 gap-2 text-xs">
                              <div>
                                <span className="text-muted-foreground">Think From:</span>
                                <span className="ml-2 font-mono">{model.thinkFrom}ms</span>
                              </div>
                              <div>
                                <span className="text-muted-foreground">Think To:</span>
                                <span className="ml-2 font-mono">{model.thinkTo}ms</span>
                              </div>
                            </div>

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
            {data.config.operationalProfile?.length > 0 && (
              <div className="space-y-2">
                <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                  <Activity className="h-4 w-4" />
                  <span>Operational Profile</span>
                </div>
                <div className="pl-6 text-sm">
                  {data.config.operationalProfile.map((p: any, i: number) => (
                    <span key={i}>
                      {p.load} users ({(p.frequency * 100).toFixed(0)}%)
                      {i < data.config.operationalProfile.length - 1 && ', '}
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
            {/* Baseline SUT */}
            <div>
              <div className="flex items-center gap-2 mb-3">
                <Badge variant="default" className="bg-blue-600">Baseline</Badge>
                <Link to={`/systems-under-test/${data.baselineSut.id}`} className="text-lg font-semibold hover:underline">
                  {data.baselineSut.name}
                </Link>
              </div>

              {data.baselineSut.description && (
                <p className="text-sm text-muted-foreground mb-3">{data.baselineSut.description}</p>
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
                    <span className="font-medium">{data.baselineSut.dockerConfig.composeFile.filename}</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 pl-5">
                    <div>
                      <span className="text-muted-foreground">App Port:</span>
                      <span className="ml-2 font-mono">{data.baselineSut.dockerConfig.appPort}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Health Check:</span>
                      <span className="ml-2 font-mono">{data.baselineSut.dockerConfig.healthCheckPath}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Startup Timeout:</span>
                      <span className="ml-2 font-mono">{data.baselineSut.dockerConfig.startupTimeoutSeconds}s</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Database Seed Configurations */}
              {data.baselineSut.databaseSeedConfigs.length > 0 && (
                <div className="mb-3 p-3 rounded-lg bg-muted/30 space-y-2">
                  <div className="flex items-center gap-2 text-sm font-medium">
                    <Database className="h-4 w-4 text-muted-foreground" />
                    <span>Database Configurations ({data.baselineSut.databaseSeedConfigs.length})</span>
                  </div>
                  <div className="pl-6 space-y-3">
                    {data.baselineSut.databaseSeedConfigs.map((dbConfig: any, idx: number) => (
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

              {/* Baseline Validation */}
              <div className="mb-3">
                {(() => {
                  const validationResult = data.baselineSutValidationResult
                  const validationState = validationResult?.validationState
                  const isPending = validationState === 'PENDING'
                  const isValid = validationState === 'VALID'
                  const isInvalid = validationState === 'INVALID'

                  return (
                    <div className="flex flex-col gap-2">
                      <Button
                        variant={isValid ? 'outline' : isInvalid ? 'destructive' : 'outline'}
                        size="sm"
                        onClick={() => handleValidateConfig(data.baselineSut.id)}
                        disabled={isPending}
                        className={isValid ? 'border-green-600 text-green-600 hover:bg-green-50 w-fit' : 'w-fit'}
                      >
                        {isPending ? (
                          <>
                            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                            Validating...
                          </>
                        ) : isValid ? (
                          <>
                            <CheckCircle2 className="h-4 w-4 mr-2 text-green-600" />
                            Re-validate
                          </>
                        ) : isInvalid ? (
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
                      {isInvalid && validationResult?.errorMessage && (
                        <div className="p-2 rounded-md bg-destructive/10 border border-destructive/20">
                          <p className="text-xs text-destructive">{validationResult.errorMessage}</p>
                        </div>
                      )}
                      {(isValid || isInvalid) && validationResult?.k6Output && (
                        <>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => toggleK6Output('baseline')}
                            className="text-xs w-fit"
                          >
                            {expandedK6Outputs.has('baseline') ? (
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
                          {expandedK6Outputs.has('baseline') && (
                            <div className="p-3 rounded-lg bg-background border">
                              <p className="text-xs font-semibold text-muted-foreground mb-2">Validation Output:</p>
                              <pre className="text-xs font-mono whitespace-pre-wrap overflow-x-auto max-h-96 overflow-y-auto p-2 rounded bg-muted/30">
                                {validationResult.k6Output}
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

            <div className="border-t" />

            {/* Target SUT */}
            <div>
              <div className="flex items-center gap-2 mb-3">
                <Badge variant="default" className="bg-purple-600">Target</Badge>
                <Link to={`/systems-under-test/${data.targetSut.id}`} className="text-lg font-semibold hover:underline">
                  {data.targetSut.name}
                </Link>
              </div>

              {data.targetSut.description && (
                <p className="text-sm text-muted-foreground mb-3">{data.targetSut.description}</p>
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
                    <span className="font-medium">{data.targetSut.dockerConfig.composeFile.filename}</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 pl-5">
                    <div>
                      <span className="text-muted-foreground">App Port:</span>
                      <span className="ml-2 font-mono">{data.targetSut.dockerConfig.appPort}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Health Check:</span>
                      <span className="ml-2 font-mono">{data.targetSut.dockerConfig.healthCheckPath}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Startup Timeout:</span>
                      <span className="ml-2 font-mono">{data.targetSut.dockerConfig.startupTimeoutSeconds}s</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Database Seed Configurations */}
              {data.targetSut.databaseSeedConfigs.length > 0 && (
                <div className="mb-3 p-3 rounded-lg bg-muted/30 space-y-2">
                  <div className="flex items-center gap-2 text-sm font-medium">
                    <Database className="h-4 w-4 text-muted-foreground" />
                    <span>Database Configurations ({data.targetSut.databaseSeedConfigs.length})</span>
                  </div>
                  <div className="pl-6 space-y-3">
                    {data.targetSut.databaseSeedConfigs.map((dbConfig: any, idx: number) => (
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

              {/* Target Validation */}
              <div className="mb-3">
                {(() => {
                  const validationResult = data.targetSutValidationResult
                  const validationState = validationResult?.validationState
                  const isPending = validationState === 'PENDING'
                  const isValid = validationState === 'VALID'
                  const isInvalid = validationState === 'INVALID'

                  return (
                    <div className="flex flex-col gap-2">
                      <Button
                        variant={isValid ? 'outline' : isInvalid ? 'destructive' : 'outline'}
                        size="sm"
                        onClick={() => handleValidateConfig(data.targetSut.id)}
                        disabled={isPending}
                        className={isValid ? 'border-green-600 text-green-600 hover:bg-green-50 w-fit' : 'w-fit'}
                      >
                        {isPending ? (
                          <>
                            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                            Validating...
                          </>
                        ) : isValid ? (
                          <>
                            <CheckCircle2 className="h-4 w-4 mr-2 text-green-600" />
                            Re-validate
                          </>
                        ) : isInvalid ? (
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
                      {isInvalid && validationResult?.errorMessage && (
                        <div className="p-2 rounded-md bg-destructive/10 border border-destructive/20">
                          <p className="text-xs text-destructive">{validationResult.errorMessage}</p>
                        </div>
                      )}
                      {(isValid || isInvalid) && validationResult?.k6Output && (
                        <>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => toggleK6Output('target')}
                            className="text-xs w-fit"
                          >
                            {expandedK6Outputs.has('target') ? (
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
                          {expandedK6Outputs.has('target') && (
                            <div className="p-3 rounded-lg bg-background border">
                              <p className="text-xs font-semibold text-muted-foreground mb-2">Validation Output:</p>
                              <pre className="text-xs font-mono whitespace-pre-wrap overflow-x-auto max-h-96 overflow-y-auto p-2 rounded bg-muted/30">
                                {validationResult.k6Output}
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
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
