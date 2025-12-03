import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { FileSelector } from '@/components/ui/file-selector'
import { useToast } from '@/components/ui/use-toast'
import { type UploadedFile } from '@/hooks/useFileUpload'
import { useCreateOperationalSetting } from '@/api/generated/operational-setting-controller/operational-setting-controller'
import { ArrowLeft, Plus, Trash2, Check, Activity, FileJson, Loader2 } from 'lucide-react'

type BehaviorModelInputMode = 'manual' | 'json' | 'auto-generate'

// Form schemas (reusing from BenchmarkCreatePage)
const keyValuePairSchema = z.object({
  key: z.string(),
  value: z.string(),
})

const behaviorModelSchema = z.object({
  id: z.string().min(1, 'ID is required'),
  actor: z.string().min(1, 'Actor name is required'),
  frequency: z.coerce.number().min(0).max(1),
  steps: z.array(
    z.object({
      method: z.string().min(1, 'Method is required'),
      path: z.string().min(1, 'Path is required'),
      operationId: z.string().min(1, 'Operation ID is required'),
      component: z.string().optional(),
      waitMsFrom: z.coerce.number().min(0, 'Wait from must be non-negative'),
      waitMsTo: z.coerce.number().min(0, 'Wait to must be non-negative'),
      headers: z.array(keyValuePairSchema).default([]),
      params: z.array(keyValuePairSchema).default([]),
      body: z.string().default('{}'),
      save: z.array(keyValuePairSchema).default([]),
    })
  ).min(1, 'At least one step is required'),
})

const benchmarkConfigSchema = z.object({
  name: z.string().min(1, 'Config name is required'),
  description: z.string().optional(),
  generateBehaviorModels: z.boolean().default(false),
  behaviorModels: z.array(behaviorModelSchema).optional(),
  behaviorModelsJson: z.string().optional(),
  operationalProfile: z.array(
    z.object({
      load: z.coerce.number().min(1, 'Load must be at least 1'),
      frequency: z.coerce.number().min(0).max(1, 'Frequency must be between 0 and 1'),
    })
  ).min(1, 'At least one operational load is required'),
}).refine((data) => {
  if (data.generateBehaviorModels) return true
  if (data.behaviorModels && data.behaviorModels.length > 0) return true
  if (data.behaviorModelsJson && data.behaviorModelsJson.trim() !== '') return true
  return false
}, {
  message: 'Either enable auto-generation, provide JSON, or define at least one behavior model manually',
  path: ['behaviorModels'],
})

type BenchmarkConfigFormData = z.infer<typeof benchmarkConfigSchema>

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
      <Label>API Request Steps</Label>

      {stepFields.map((field: any, stepIndex) => (
        <Card key={field.id} className="p-3 bg-muted/30">
          <div className="space-y-3">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium">Step {stepIndex + 1}</span>
              <Button type="button" variant="ghost" size="sm" onClick={() => removeStep(stepIndex)}>
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>

            <div className="grid grid-cols-3 gap-3">
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

              <div className="space-y-2">
                <Label>Method</Label>
                <Select
                  defaultValue={field.method}
                  onValueChange={(value) => {
                    const input = document.getElementById(`behavior-${behaviorIndex}-step-${stepIndex}-method-hidden`) as HTMLInputElement
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
                  id={`behavior-${behaviorIndex}-step-${stepIndex}-method-hidden`}
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

            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-2">
                <Label>Component (optional)</Label>
                <Input
                  {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.component`)}
                  placeholder="user-service"
                />
              </div>
              <div className="space-y-2">
                <Label>Wait From (ms)</Label>
                <Input
                  type="number"
                  {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.waitMsFrom`)}
                  placeholder="0"
                />
              </div>
              <div className="space-y-2">
                <Label>Wait To (ms)</Label>
                <Input
                  type="number"
                  {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.waitMsTo`)}
                  placeholder="0"
                />
              </div>
            </div>

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
            </div>
          </div>
        </Card>
      ))}

      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={() => appendStep({ method: 'GET', path: '/', operationId: '', component: '', waitMsFrom: 0, waitMsTo: 0, headers: [], params: [], body: '{}', save: [] })}
      >
        <Plus className="h-4 w-4 mr-2" />
        Add Step
      </Button>
    </div>
  )
}

export function OperationalSettingCreatePage() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const createOperationalSetting = useCreateOperationalSetting()

  const [openApiFile, setOpenApiFile] = useState<UploadedFile | null>(null)
  const [behaviorModelInputMode, setBehaviorModelInputMode] = useState<BehaviorModelInputMode>('manual')
  const [jsonValidationError, setJsonValidationError] = useState<string | null>(null)

  const form = useForm<BenchmarkConfigFormData>({
    resolver: zodResolver(benchmarkConfigSchema),
    defaultValues: {
      name: '',
      description: '',
      generateBehaviorModels: false,
      behaviorModels: [],
      behaviorModelsJson: '',
      operationalProfile: [{ load: 25, frequency: 1.0 }],
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

  const keyValuePairsToObject = (pairs: { key: string; value: string }[]): Record<string, string> => {
    return pairs.reduce((acc, { key, value }) => {
      if (key.trim()) {
        acc[key] = value
      }
      return acc
    }, {} as Record<string, string>)
  }

  const validateBehaviorModelsJson = (jsonString: string) => {
    if (!jsonString || jsonString.trim() === '') {
      setJsonValidationError(null)
      return
    }

    try {
      const parsed = JSON.parse(jsonString)
      if (!Array.isArray(parsed)) {
        setJsonValidationError('Behavior models must be an array')
        return
      }

      for (let i = 0; i < parsed.length; i++) {
        const model = parsed[i]
        if (!model.id || !model.actor || typeof model.frequency !== 'number') {
          setJsonValidationError(`Model at index ${i} is missing required fields`)
          return
        }
        if (!Array.isArray(model.steps) || model.steps.length === 0) {
          setJsonValidationError(`Model at index ${i} must have at least one step`)
          return
        }
        for (let j = 0; j < model.steps.length; j++) {
          const step = model.steps[j]
          if (!step.method || !step.path || !step.operationId) {
            setJsonValidationError(`Model ${model.id}, step ${j}: missing required fields`)
            return
          }
        }
      }

      setJsonValidationError(null)
    } catch (e) {
      if (e instanceof SyntaxError) {
        setJsonValidationError(`Invalid JSON syntax: ${e.message}`)
      } else {
        setJsonValidationError(`Validation error: ${e instanceof Error ? e.message : String(e)}`)
      }
    }
  }

  const onSubmit = async (data: BenchmarkConfigFormData) => {
    if (!openApiFile) {
      toast({
        variant: 'destructive',
        title: 'OpenAPI file is required',
      })
      return
    }

    if (behaviorModelInputMode === 'json' && jsonValidationError) {
      toast({
        variant: 'destructive',
        title: 'Invalid behavior models JSON',
        description: jsonValidationError,
      })
      return
    }

    try {
      let behaviorModels: any[] = []

      if (behaviorModelInputMode === 'json' && data.behaviorModelsJson) {
        behaviorModels = JSON.parse(data.behaviorModelsJson)
      } else if (behaviorModelInputMode === 'manual' && data.behaviorModels && data.behaviorModels.length > 0) {
        behaviorModels = data.behaviorModels.map((model: any) => {
          const steps = model.steps.map((step: any) => ({
            method: step.method,
            path: step.path,
            operationId: step.operationId,
            component: step.component || undefined,
            waitMsFrom: Number(step.waitMsFrom) || 0,
            waitMsTo: Number(step.waitMsTo) || 0,
            headers: keyValuePairsToObject(step.headers || []),
            params: keyValuePairsToObject(step.params || []),
            body: step.body && step.body.trim() !== '' ? JSON.parse(step.body) : {},
            save: keyValuePairsToObject(step.save || []),
          }))
          return {
            id: model.id,
            actor: model.actor,
            frequency: model.frequency,
            steps,
          }
        })
      }

      const freqSum = data.operationalProfile.reduce((sum: number, p: any) => sum + Number(p.frequency), 0)
      if (Math.abs(freqSum - 1.0) > 0.001) {
        toast({
          variant: 'destructive',
          title: 'Invalid operational profile',
          description: `Frequencies must sum to 1.0 (current sum: ${freqSum.toFixed(3)})`,
        })
        return
      }

      // Transform operationalProfile from array to object
      const operationalProfile = data.operationalProfile.reduce((acc: Record<string, number>, p: any) => {
        acc[p.load.toString()] = Number(p.frequency)
        return acc
      }, {})

      const result = await createOperationalSetting.mutateAsync({
        data: {
          name: data.name,
          description: data.description || undefined,
          openApiFileId: openApiFile.fileId,
          usageProfile: behaviorModels,
          operationalProfile,
        },
      })

      toast({
        title: 'Config created',
        description: 'Your benchmark config has been created successfully',
      })

      navigate(`/operational-settings/${(result as any).operationalSettingId}`)
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to create config',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/operational-settings">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold">Create Operational Setting</h1>
          <p className="text-muted-foreground">Create a reusable load test configuration</p>
        </div>
      </div>

      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        {/* Combined Card */}
        <Card>
          <CardContent className="space-y-6 pt-6">
            {/* Basic Info and OpenAPI File in same row */}
            <div className="grid grid-cols-2 gap-6">
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="name">Name *</Label>
                  <Input id="name" {...form.register('name')} placeholder="Standard Load Test" />
                  {form.formState.errors.name && (
                    <p className="text-sm text-destructive">{String(form.formState.errors.name.message)}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="description">Description (optional)</Label>
                  <Textarea
                    id="description"
                    {...form.register('description')}
                    placeholder="Standard load test configuration for REST APIs"
                    rows={3}
                  />
                </div>
              </div>

              <div>
                <FileSelector
                  id="openapi-file"
                  label="OpenAPI Specification File"
                  accept=".json,.yaml,.yml"
                  required
                  onFileSelected={setOpenApiFile}
                  selectedFile={openApiFile}
                  mimeTypeFilter="json"
                />
              </div>
            </div>

            {/* Behavior Models */}
            <div className="space-y-4">
              <div className="space-y-3">
                <Label>Behavior Models Input Method</Label>
                <div className="grid grid-cols-3 gap-3">
                    <Button
                        type="button"
                        variant={behaviorModelInputMode === 'manual' ? 'default' : 'outline'}
                        className="w-full"
                        onClick={() => {
                            setBehaviorModelInputMode('manual')
                            form.setValue('generateBehaviorModels', false)
                            setJsonValidationError(null)
                        }}
                    >
                        Manual Input
                    </Button>
                  <Button
                    type="button"
                    variant={behaviorModelInputMode === 'json' ? 'default' : 'outline'}
                    className="w-full"
                    onClick={() => {
                      setBehaviorModelInputMode('json')
                      form.setValue('generateBehaviorModels', false)
                      const currentJson = form.watch('behaviorModelsJson')
                      if (currentJson) validateBehaviorModelsJson(currentJson)
                    }}
                  >
                    <FileJson className="h-4 w-4 mr-2" />
                    Raw JSON
                  </Button>
                    <Button
                        type="button"
                        variant={behaviorModelInputMode === 'auto-generate' ? 'default' : 'outline'}
                        className="w-full"
                        onClick={() => {
                            setBehaviorModelInputMode('auto-generate')
                            form.setValue('generateBehaviorModels', true)
                            setJsonValidationError(null)
                        }}
                    >
                        <Activity className="h-4 w-4 mr-2" />
                        Auto-generate
                    </Button>
                </div>
              </div>

              {behaviorModelInputMode === 'auto-generate' && (
                <div className="p-4 rounded-lg bg-blue-50 border border-blue-200">
                  <p className="text-sm text-blue-900">
                    Behavior models will be automatically generated from the OpenAPI specification file.
                  </p>
                </div>
              )}

              {behaviorModelInputMode === 'json' && (
                <div className="space-y-2">
                  <Label htmlFor="behavior-models-json">Behavior Models JSON</Label>
                  <Textarea
                    id="behavior-models-json"
                    {...form.register('behaviorModelsJson', {
                      onChange: (e) => validateBehaviorModelsJson(e.target.value)
                    })}
                    placeholder={`[{"id": "checkout-flow", "actor": "Customer", "usageProfile": 0.7, "steps": [{"method": "GET", "path": "/api/products", "operationId": "listProducts", "headers": {}, "params": {}, "body": {}, "save": {}}]}]`}
                    rows={10}
                    className={`font-mono text-xs ${jsonValidationError ? 'border-destructive' : ''}`}
                  />
                  {jsonValidationError && (
                    <div className="flex items-start gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive">
                      <p className="text-sm text-destructive">{jsonValidationError}</p>
                    </div>
                  )}
                  {!jsonValidationError && form.watch('behaviorModelsJson') && form.watch('behaviorModelsJson')?.trim() !== '' && (
                    <div className="flex items-center gap-2 p-2 rounded-lg bg-green-50 border border-green-200">
                      <Check className="h-4 w-4 text-green-600" />
                      <p className="text-sm text-green-900">Valid JSON format</p>
                    </div>
                  )}
                </div>
              )}

              {behaviorModelInputMode === 'manual' && (
                <div className="space-y-4">
                  <Label>Behavior Models</Label>

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

                        <div className="space-y-2">
                          <Label>Behavior Model Frequency (0-1)</Label>
                          <Input type="number" step="0.01" min="0" max="1" {...form.register(`behaviorModels.${index}.frequency`)} />
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

                  {behaviorFields.length === 0 && (
                    <p className="text-sm text-muted-foreground italic">
                      No behavior models defined. Click "Add Model" to create one.
                    </p>
                  )}

                  {form.formState.errors.behaviorModels && (
                    <p className="text-sm text-destructive">{form.formState.errors.behaviorModels.message as any}</p>
                  )}

                  <div className="flex justify-end">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => appendBehavior({
                        id: '',
                        actor: '',
                        frequency: 0.5,
                        steps: [{ method: 'GET', path: '/', operationId: '', component: '', waitMsFrom: 0, waitMsTo: 0, headers: [], params: [], body: '{}', save: [] }],
                      })}
                    >
                      <Plus className="h-4 w-4 mr-2" />
                      Add Model
                    </Button>
                  </div>
                </div>
              )}
            </div>

            {/* Operational Profile */}
            <div className="space-y-4">
              <Label>Operational Profile</Label>

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

              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-muted-foreground">Total frequency:</span>
                  <span className={`text-sm font-medium ${
                    (() => {
                      const sum = form.watch('operationalProfile')?.reduce((acc: number, p: any) => acc + Number(p.frequency || 0), 0) || 0
                      return Math.abs(sum - 1.0) < 0.001 ? 'text-green-600' : 'text-orange-600'
                    })()
                  }`}>
                    {(form.watch('operationalProfile')?.reduce((acc: number, p: any) => acc + Number(p.frequency || 0), 0) || 0).toFixed(3)}
                  </span>
                  {(() => {
                    const sum = form.watch('operationalProfile')?.reduce((acc: number, p: any) => acc + Number(p.frequency || 0), 0) || 0
                    return Math.abs(sum - 1.0) < 0.001 ? (
                      <Check className="h-4 w-4 text-green-600" />
                    ) : (
                      <span className="text-xs text-orange-600">(must sum to 1.0)</span>
                    )
                  })()}
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => appendOperationalProfile({ load: 50, frequency: 0 })}
                >
                  <Plus className="h-4 w-4 mr-2" />
                  Add Load
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="flex justify-end gap-4">
          <Link to="/operational-settings">
            <Button type="button" variant="outline">Cancel</Button>
          </Link>
          <Button type="submit" disabled={createOperationalSetting.isPending}>
            {createOperationalSetting.isPending ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Creating...
              </>
            ) : (
              'Create Config'
            )}
          </Button>
        </div>
      </form>
    </div>
  )
}
