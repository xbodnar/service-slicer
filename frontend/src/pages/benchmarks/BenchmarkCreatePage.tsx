import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { v4 as uuidv4 } from 'uuid'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { FileSelector } from '@/components/ui/file-selector'
import { Badge } from '@/components/ui/badge'
import { Stepper, StepContent, StepNavigation, type Step } from '@/components/ui/stepper'
import { useToast } from '@/components/ui/use-toast'
import { type UploadedFile } from '@/hooks/useFileUpload'
import { useCreateBenchmark } from '@/hooks/useBenchmarks'
import { useListSystemsUnderTest } from '@/api/generated/system-under-test-controller/system-under-test-controller'
import { ArrowLeft, Plus, Trash2, Check, Server, Activity, FileCode, Loader2, FileJson } from 'lucide-react'

type BehaviorModelInputMode = 'auto-generate' | 'json' | 'manual'

// Step definitions
const STEPS: Step[] = [
  { id: 'basic', title: 'Basic Info' },
  { id: 'loadtest', title: 'Load Test Config' },
  { id: 'suts', title: 'Select Systems' },
  { id: 'review', title: 'Review' },
]

// Form schemas
const basicInfoSchema = z.object({
  name: z.string().min(1, 'Benchmark name is required'),
  description: z.string().optional(),
})

const keyValuePairSchema = z.object({
  key: z.string(),
  value: z.string(),
})

const behaviorModelSchema = z.object({
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

const loadTestConfigSchema = z.object({
  generateBehaviorModels: z.boolean().default(false),
  behaviorModels: z.array(behaviorModelSchema).optional(),
  behaviorModelsJson: z.string().optional(),
  testDuration: z.string()
    .regex(/^\d+[smh]$/, 'Duration must be in format like "30s", "5m", or "1h"')
    .optional()
    .or(z.literal('')),
  operationalProfile: z.array(
    z.object({
      load: z.coerce.number().min(1, 'Load must be at least 1'),
      frequency: z.coerce.number().min(0).max(1, 'Frequency must be between 0 and 1'),
    })
  ).min(1, 'At least one operational load is required'),
})

const benchmarkSchema = z.object({
  ...basicInfoSchema.shape,
  ...loadTestConfigSchema.shape,
  baselineSutId: z.string().min(1, 'Baseline system is required'),
  targetSutId: z.string().min(1, 'Target system is required'),
}).refine((data) => {
  // If auto-generate is enabled, validation passes
  if (data.generateBehaviorModels) {
    return true
  }
  // If manual mode, check for behaviorModels
  if (data.behaviorModels && data.behaviorModels.length > 0) {
    return true
  }
  // If JSON mode, check for behaviorModelsJson
  if (data.behaviorModelsJson && data.behaviorModelsJson.trim() !== '') {
    return true
  }
  return false
}, {
  message: 'Either enable auto-generation, provide JSON, or define at least one behavior model manually',
  path: ['behaviorModels'],
}).refine((data) => data.baselineSutId !== data.targetSutId, {
  message: 'Baseline and target systems must be different',
  path: ['targetSutId'],
})

type BenchmarkFormData = z.infer<typeof benchmarkSchema>

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
    </div>
  )
}

export function BenchmarkCreatePage() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const createBenchmark = useCreateBenchmark()
  const { data: sutsData, isLoading: sutsLoading } = useListSystemsUnderTest()

  const [currentStep, setCurrentStep] = useState(0)
  const [openApiFile, setOpenApiFile] = useState<UploadedFile | null>(null)
  const [behaviorModelInputMode, setBehaviorModelInputMode] = useState<BehaviorModelInputMode>('auto-generate')
  const [jsonValidationError, setJsonValidationError] = useState<string | null>(null)

  const form = useForm<BenchmarkFormData>({
    resolver: zodResolver(benchmarkSchema),
    defaultValues: {
      name: '',
      description: '',
      generateBehaviorModels: true,
      behaviorModels: [],
      behaviorModelsJson: '',
      testDuration: '',
      operationalProfile: [{ load: 25, frequency: 1.0 }],
      baselineSutId: '',
      targetSutId: '',
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

  // Helper to convert key-value pairs array to object
  const keyValuePairsToObject = (pairs: { key: string; value: string }[]): Record<string, string> => {
    return pairs.reduce((acc, { key, value }) => {
      if (key.trim()) {
        acc[key] = value
      }
      return acc
    }, {} as Record<string, string>)
  }

  // Validate JSON input in real-time
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

      // Basic schema validation
      for (let i = 0; i < parsed.length; i++) {
        const model = parsed[i]
        if (!model.id) {
          setJsonValidationError(`Model at index ${i} is missing required field: id`)
          return
        }
        if (!model.actor) {
          setJsonValidationError(`Model at index ${i} is missing required field: actor`)
          return
        }
        if (typeof model.usageProfile !== 'number') {
          setJsonValidationError(`Model at index ${i} is missing required field: usageProfile`)
          return
        }
        if (!Array.isArray(model.steps) || model.steps.length === 0) {
          setJsonValidationError(`Model at index ${i} must have at least one step`)
          return
        }

        // Validate steps
        for (let j = 0; j < model.steps.length; j++) {
          const step = model.steps[j]
          if (!step.method || !step.path || !step.operationId) {
            setJsonValidationError(`Model ${model.id}, step ${j}: missing required fields (method, path, operationId)`)
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

  const onSubmit = async (data: BenchmarkFormData) => {
    if (!openApiFile) {
      toast({
        variant: 'destructive',
        title: 'OpenAPI file is required',
      })
      return
    }

    // Check for JSON validation errors
    if (behaviorModelInputMode === 'json' && jsonValidationError) {
      toast({
        variant: 'destructive',
        title: 'Invalid behavior models JSON',
        description: jsonValidationError,
      })
      return
    }

    try {
      // Process behavioral models
      let behaviorModels: any[] = []

      // Handle JSON input mode
      if (behaviorModelInputMode === 'json' && data.behaviorModelsJson) {
        try {
          behaviorModels = JSON.parse(data.behaviorModelsJson)
          if (!Array.isArray(behaviorModels)) {
            throw new Error('Behavior models must be an array')
          }
        } catch (e) {
          throw new Error(`Invalid JSON format: ${e instanceof Error ? e.message : String(e)}`)
        }
      }
      // Handle manual input mode
      else if (behaviorModelInputMode === 'manual' && data.behaviorModels && data.behaviorModels.length > 0) {
        behaviorModels = data.behaviorModels.map((model: any) => {
          const steps = model.steps.map((step: any) => {
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
      }
      // Auto-generate mode - behaviorModels will be empty array

      // Validate operational profile
      const freqSum = data.operationalProfile.reduce((sum: number, p: any) => sum + Number(p.frequency), 0)
      if (Math.abs(freqSum - 1.0) > 0.001) {
        toast({
          variant: 'destructive',
          title: 'Invalid operational profile',
          description: `Frequencies must sum to 1.0 (current sum: ${freqSum.toFixed(3)})`,
        })
        return
      }

      const result = await createBenchmark.mutateAsync({
        data: {
          name: data.name,
          description: data.description || undefined,
          benchmarkConfig: {
            id: uuidv4(),
            openApiFileId: openApiFile.fileId,
            behaviorModels,
            operationalProfile: data.operationalProfile,
            testDuration: data.testDuration && data.testDuration.trim() !== '' ? data.testDuration : undefined,
          },
          baselineSutId: data.baselineSutId,
          targetSutId: data.targetSutId,
        },
      })

      toast({
        title: 'Benchmark created',
        description: 'Your benchmark has been created successfully',
      })

      navigate(`/benchmarks/${result.benchmarkId}`)
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to create benchmark',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const handleSubmit = () => {
    form.handleSubmit(onSubmit)()
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
    appendOperationalProfile({ load: 50, frequency: 0 })
  }

  // Get available systems
  const systems = (sutsData as any)?.systemsUnderTest || []

  // Render step content
  const renderStepContent = () => {
    switch (currentStep) {
      case 0:
        return (
          <Card>
            <CardHeader>
              <CardTitle>Benchmark Details</CardTitle>
              <CardDescription>Basic information about the benchmark</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name">Name *</Label>
                <Input id="name" {...form.register('name')} placeholder="Performance Comparison Test" />
                {form.formState.errors.name && (
                  <p className="text-sm text-destructive">{String(form.formState.errors.name.message)}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">Description (optional)</Label>
                <Textarea
                  id="description"
                  {...form.register('description')}
                  placeholder="Compare performance between monolithic and microservices architecture"
                  rows={3}
                />
              </div>
            </CardContent>
          </Card>
        )

      case 1:
        return (
          <Card>
            <CardHeader>
              <CardTitle>Load Test Configuration</CardTitle>
              <CardDescription>Configure the load test parameters</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* OpenAPI File */}
              <FileSelector
                id="openapi-file"
                label="OpenAPI Specification File"
                accept=".json,.yaml,.yml"
                required
                onFileSelected={setOpenApiFile}
                selectedFile={openApiFile}
                mimeTypeFilter="json"
              />

              {/* Test Duration */}
              <div className="space-y-2">
                <Label htmlFor="test-duration">Test Duration (optional)</Label>
                <Input
                  id="test-duration"
                  {...form.register('testDuration')}
                  placeholder="30s"
                  className="max-w-xs"
                />
                {form.formState.errors.testDuration && (
                  <p className="text-sm text-destructive">{String(form.formState.errors.testDuration.message)}</p>
                )}
                <p className="text-xs text-muted-foreground">
                  Duration format: <span className="font-mono">30s</span> (seconds), <span className="font-mono">5m</span> (minutes),
                  or <span className="font-mono">1h</span> (hours). Leave empty for default.
                </p>
              </div>

              {/* Behavior Models */}
              <div className="space-y-4">
                <div className="space-y-3">
                  <Label>Behavior Models Input Method</Label>
                  <div className="grid grid-cols-3 gap-3">
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
                    <Button
                      type="button"
                      variant={behaviorModelInputMode === 'json' ? 'default' : 'outline'}
                      className="w-full"
                      onClick={() => {
                        setBehaviorModelInputMode('json')
                        form.setValue('generateBehaviorModels', false)
                        // Re-validate current JSON if any
                        const currentJson = form.watch('behaviorModelsJson')
                        if (currentJson) {
                          validateBehaviorModelsJson(currentJson)
                        }
                      }}
                    >
                      <FileJson className="h-4 w-4 mr-2" />
                      Raw JSON
                    </Button>
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
                      <FileCode className="h-4 w-4 mr-2" />
                      Manual Input
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
                      placeholder={`[\n  {\n    "id": "checkout-flow",\n    "actor": "Customer",\n    "usageProfile": 0.7,\n    "steps": [\n      {\n        "method": "GET",\n        "path": "/api/products",\n        "operationId": "listProducts",\n        "headers": {},\n        "params": {},\n        "body": {},\n        "save": {}\n      }\n    ],\n    "thinkFrom": 1000,\n    "thinkTo": 3000\n  }\n]`}
                      rows={15}
                      className={`font-mono text-xs ${jsonValidationError ? 'border-destructive' : ''}`}
                    />
                    {jsonValidationError && (
                      <div className="flex items-start gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive">
                        <p className="text-sm text-destructive">{jsonValidationError}</p>
                      </div>
                    )}
                    {!jsonValidationError && form.watch('behaviorModelsJson') && form.watch('behaviorModelsJson').trim() !== '' && (
                      <div className="flex items-center gap-2 p-2 rounded-lg bg-green-50 border border-green-200">
                        <Check className="h-4 w-4 text-green-600" />
                        <p className="text-sm text-green-900">Valid JSON format</p>
                      </div>
                    )}
                    <p className="text-xs text-muted-foreground">
                      Paste the complete JSON array of behavior models. Must be valid JSON matching the BehaviorModel schema.
                    </p>
                  </div>
                )}

                {behaviorModelInputMode === 'manual' && (
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
                              <Label>Usage Profile (0-1)</Label>
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

                    {behaviorFields.length === 0 && (
                      <p className="text-sm text-muted-foreground italic">
                        No behavior models defined. Click "Add Model" to create one.
                      </p>
                    )}

                    {form.formState.errors.behaviorModels && (
                      <p className="text-sm text-destructive">{form.formState.errors.behaviorModels.message as any}</p>
                    )}
                  </div>
                )}
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
        )

      case 2:
        return (
          <Card>
            <CardHeader>
              <CardTitle>Select Systems Under Test</CardTitle>
              <CardDescription>
                Choose the baseline and target systems to compare.
                {systems.length === 0 && (
                  <span className="block mt-2 text-destructive">
                    No systems available. <Link to="/systems-under-test/new" className="underline">Create a system</Link> first.
                  </span>
                )}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {sutsLoading ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                </div>
              ) : (
                <>
                  <div className="space-y-2">
                    <Label htmlFor="baseline-sut">Baseline System *</Label>
                    <Select
                      value={form.watch('baselineSutId')}
                      onValueChange={(value) => form.setValue('baselineSutId', value)}
                    >
                      <SelectTrigger id="baseline-sut">
                        <SelectValue placeholder="Select baseline system" />
                      </SelectTrigger>
                      <SelectContent>
                        {systems.map((system: any) => (
                          <SelectItem key={system.id} value={system.id}>
                            <div className="flex items-center gap-2">
                              <Server className="h-4 w-4" />
                              {system.name}
                              {system.description && (
                                <span className="text-xs text-muted-foreground">- {system.description}</span>
                              )}
                            </div>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {form.formState.errors.baselineSutId && (
                      <p className="text-sm text-destructive">{String(form.formState.errors.baselineSutId.message)}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="target-sut">Target System *</Label>
                    <Select
                      value={form.watch('targetSutId')}
                      onValueChange={(value) => form.setValue('targetSutId', value)}
                    >
                      <SelectTrigger id="target-sut">
                        <SelectValue placeholder="Select target system" />
                      </SelectTrigger>
                      <SelectContent>
                        {systems.map((system: any) => (
                          <SelectItem key={system.id} value={system.id}>
                            <div className="flex items-center gap-2">
                              <Server className="h-4 w-4" />
                              {system.name}
                              {system.description && (
                                <span className="text-xs text-muted-foreground">- {system.description}</span>
                              )}
                            </div>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {form.formState.errors.targetSutId && (
                      <p className="text-sm text-destructive">{String(form.formState.errors.targetSutId.message)}</p>
                    )}
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        )

      case 3:
        const formData = form.getValues()
        const baselineSut = systems.find((s: any) => s.id === formData.baselineSutId)
        const targetSut = systems.find((s: any) => s.id === formData.targetSutId)

        return (
          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Check className="h-5 w-5 text-green-600" />
                  Review Your Benchmark
                </CardTitle>
                <CardDescription>Please review all details before creating the benchmark</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                {/* Basic Info Summary */}
                <div className="space-y-2">
                  <h4 className="font-semibold flex items-center gap-2">
                    <FileCode className="h-4 w-4" />
                    Basic Information
                  </h4>
                  <div className="pl-6 space-y-1 text-sm">
                    <p><span className="text-muted-foreground">Name:</span> {formData.name}</p>
                    {formData.description && (
                      <p><span className="text-muted-foreground">Description:</span> {formData.description}</p>
                    )}
                  </div>
                </div>

                {/* Load Test Config Summary */}
                <div className="space-y-2">
                  <h4 className="font-semibold flex items-center gap-2">
                    <Activity className="h-4 w-4" />
                    Load Test Configuration
                  </h4>
                  <div className="pl-6 space-y-1 text-sm">
                    <p><span className="text-muted-foreground">OpenAPI File:</span> {openApiFile?.filename}</p>
                    <p>
                      <span className="text-muted-foreground">Test Duration:</span>{' '}
                      {formData.testDuration && formData.testDuration.trim() !== '' ? formData.testDuration : 'Default'}
                    </p>
                    <p>
                      <span className="text-muted-foreground">Behavior Models:</span>{' '}
                      {behaviorModelInputMode === 'auto-generate' && 'Auto-generated from OpenAPI spec'}
                      {behaviorModelInputMode === 'json' && 'Defined via raw JSON'}
                      {behaviorModelInputMode === 'manual' && `${formData.behaviorModels?.length || 0} manually defined`}
                    </p>
                    <p>
                      <span className="text-muted-foreground">Operational Profile:</span>{' '}
                      {formData.operationalProfile.map((p: any) => `${p.load} users (${(Number(p.frequency) * 100).toFixed(0)}%)`).join(', ')}
                    </p>
                  </div>
                </div>

                {/* Systems Under Test Summary */}
                <div className="space-y-2">
                  <h4 className="font-semibold flex items-center gap-2">
                    <Server className="h-4 w-4" />
                    Systems Under Test
                  </h4>
                  <div className="pl-6 space-y-3">
                    <div className="p-3 rounded-lg bg-blue-50 border border-blue-200 text-sm">
                      <div className="flex items-center gap-2 mb-1">
                        <Badge variant="default" className="bg-blue-600">Baseline</Badge>
                        <span className="font-medium">{baselineSut?.name || 'Not selected'}</span>
                      </div>
                      {baselineSut?.description && (
                        <p className="text-xs text-muted-foreground">{baselineSut.description}</p>
                      )}
                    </div>

                    <div className="p-3 rounded-lg bg-purple-50 border border-purple-200 text-sm">
                      <div className="flex items-center gap-2 mb-1">
                        <Badge variant="default" className="bg-purple-600">Target</Badge>
                        <span className="font-medium">{targetSut?.name || 'Not selected'}</span>
                      </div>
                      {targetSut?.description && (
                        <p className="text-xs text-muted-foreground">{targetSut.description}</p>
                      )}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        )

      default:
        return null
    }
  }

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/benchmarks">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold">Create Benchmark</h1>
          <p className="text-muted-foreground">Set up a new performance benchmark</p>
        </div>
      </div>

      <Stepper steps={STEPS} currentStep={currentStep} />
      <StepContent>{renderStepContent()}</StepContent>
      <StepNavigation
        currentStep={currentStep}
        totalSteps={STEPS.length}
        onNext={() => setCurrentStep((prev) => Math.min(prev + 1, STEPS.length - 1))}
        onPrevious={() => setCurrentStep((prev) => Math.max(prev - 1, 0))}
        onSubmit={handleSubmit}
        isSubmitting={createBenchmark.isPending}
        canProceed={
          currentStep === 0
            ? !!form.watch('name')
            : currentStep === 1
              ? !!openApiFile && (behaviorModelInputMode !== 'json' || !jsonValidationError)
              : true
        }
      />
    </div>
  )
}
