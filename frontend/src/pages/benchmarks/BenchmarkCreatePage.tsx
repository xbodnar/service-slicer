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
import { ArrowLeft, Plus, Trash2, Check, Server, Activity, FileCode } from 'lucide-react'

// Step definitions
const STEPS: Step[] = [
  { id: 'basic', title: 'Basic Info' },
  { id: 'loadtest', title: 'Load Test Config' },
  { id: 'suts', title: 'Systems Under Test' },
  { id: 'review', title: 'Review' },
]

// Form schemas for each step
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
  operationalProfile: z.array(
    z.object({
      load: z.coerce.number().min(1, 'Load must be at least 1'),
      frequency: z.coerce.number().min(0).max(1, 'Frequency must be between 0 and 1'),
    })
  ).min(1, 'At least one operational load is required'),
})

const databaseSeedConfigSchema = z.object({
  id: z.string(),
  dbContainerName: z.string().min(1, 'DB container name is required'),
  dbPort: z.coerce.number().min(1, 'DB port is required'),
  dbName: z.string().min(1, 'Database name is required'),
  dbUsername: z.string().min(1, 'DB username is required'),
})

const sutSchema = z.object({
  id: z.string(),
  name: z.string().min(1, 'System name is required'),
  description: z.string().optional(),
  isBaseline: z.boolean().default(false),
  healthCheckPath: z.string().default('/actuator/health'),
  appPort: z.coerce.number().default(8080),
  startupTimeoutSeconds: z.coerce.number().default(180),
  // Database seed configs (multiple DBs for microservices)
  databaseSeedConfigs: z.array(databaseSeedConfigSchema).default([]),
})

const benchmarkSchema = z.object({
  ...basicInfoSchema.shape,
  ...loadTestConfigSchema.shape,
  systemsUnderTest: z.array(sutSchema).min(1, 'At least one system under test is required'),
}).refine((data) => {
  if (!data.generateBehaviorModels && (!data.behaviorModels || data.behaviorModels.length === 0)) {
    return false
  }
  return true
}, {
  message: 'Either enable auto-generation or define at least one behavior model',
  path: ['behaviorModels'],
})

type BenchmarkFormData = z.infer<typeof benchmarkSchema>

interface SystemFiles {
  composeFile: UploadedFile | null
  sqlSeedFiles: (UploadedFile | null)[]
}

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
          onClick={() => appendStep({ method: 'GET', path: '/', headers: [], params: [], body: '{}', save: [] })}
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
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => removeStep(stepIndex)}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor={`behavior-${behaviorIndex}-step-${stepIndex}-method`}>Method</Label>
                <Select
                  defaultValue={field.method}
                  onValueChange={(value) => {
                    const input = document.getElementById(`behavior-${behaviorIndex}-step-${stepIndex}-method-hidden`) as HTMLInputElement
                    if (input) input.value = value
                  }}
                >
                  <SelectTrigger id={`behavior-${behaviorIndex}-step-${stepIndex}-method`}>
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
                <Label htmlFor={`behavior-${behaviorIndex}-step-${stepIndex}-path`}>Path</Label>
                <Input
                  id={`behavior-${behaviorIndex}-step-${stepIndex}-path`}
                  {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.path`)}
                  placeholder="/api/resource"
                />
              </div>
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
              <Label htmlFor={`behavior-${behaviorIndex}-step-${stepIndex}-body`}>Body (JSON)</Label>
              <Textarea
                id={`behavior-${behaviorIndex}-step-${stepIndex}-body`}
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

interface DatabaseSeedConfigListProps {
  systemIndex: number
  control: any
  register: any
  onFileSelected: (systemIndex: number, dbConfigIndex: number, file: UploadedFile | null) => void
  sqlSeedFiles: (UploadedFile | null)[]
}

function DatabaseSeedConfigList({ systemIndex, control, register, onFileSelected, sqlSeedFiles }: DatabaseSeedConfigListProps) {
  const { fields, append, remove } = useFieldArray({
    control,
    name: `systemsUnderTest.${systemIndex}.databaseSeedConfigs`,
  })

  const handleAddDbConfig = () => {
    append({
      id: uuidv4(),
      dbContainerName: '',
      dbPort: 5432,
      dbName: '',
      dbUsername: '',
    })
  }

  return (
    <div className="space-y-4 p-4 rounded-lg border bg-muted/30">
      <div className="flex items-center justify-between">
        <Label className="text-sm font-semibold">Database Seed Configurations (optional)</Label>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleAddDbConfig}
        >
          <Plus className="h-4 w-4 mr-2" />
          Add Database
        </Button>
      </div>

      {fields.length === 0 && (
        <p className="text-xs text-muted-foreground italic">No database configurations. Add one if this system requires database seeding.</p>
      )}

      {fields.map((field, dbIndex) => (
        <Card key={field.id} className="p-4 bg-background">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <Label className="text-sm font-medium">Database {dbIndex + 1}</Label>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => remove(dbIndex)}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>

            <FileSelector
              id={`sql-seed-file-${systemIndex}-${dbIndex}`}
              label="SQL Seed File *"
              accept=".sql"
              required
              onFileSelected={(file) => onFileSelected(systemIndex, dbIndex, file)}
              selectedFile={sqlSeedFiles[dbIndex] || null}
              mimeTypeFilter="sql"
            />

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor={`db-container-${systemIndex}-${dbIndex}`}>DB Container Name *</Label>
                <Input
                  id={`db-container-${systemIndex}-${dbIndex}`}
                  {...register(`systemsUnderTest.${systemIndex}.databaseSeedConfigs.${dbIndex}.dbContainerName`)}
                  placeholder="postgres"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor={`db-port-${systemIndex}-${dbIndex}`}>DB Port *</Label>
                <Input
                  id={`db-port-${systemIndex}-${dbIndex}`}
                  type="number"
                  {...register(`systemsUnderTest.${systemIndex}.databaseSeedConfigs.${dbIndex}.dbPort`)}
                  placeholder="5432"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor={`db-name-${systemIndex}-${dbIndex}`}>Database Name *</Label>
                <Input
                  id={`db-name-${systemIndex}-${dbIndex}`}
                  {...register(`systemsUnderTest.${systemIndex}.databaseSeedConfigs.${dbIndex}.dbName`)}
                  placeholder="mydb"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor={`db-username-${systemIndex}-${dbIndex}`}>DB Username *</Label>
                <Input
                  id={`db-username-${systemIndex}-${dbIndex}`}
                  {...register(`systemsUnderTest.${systemIndex}.databaseSeedConfigs.${dbIndex}.dbUsername`)}
                  placeholder="postgres"
                />
              </div>
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

  const [currentStep, setCurrentStep] = useState(0)
  const [openApiFile, setOpenApiFile] = useState<UploadedFile | null>(null)
  const [systemFiles, setSystemFiles] = useState<SystemFiles[]>([
    { composeFile: null, sqlSeedFiles: [] },
  ])

  const form = useForm<BenchmarkFormData>({
    resolver: zodResolver(benchmarkSchema),
    defaultValues: {
      name: '',
      description: '',
      generateBehaviorModels: false,
      behaviorModels: [],
      operationalProfile: [{ load: 25, frequency: 1.0 }],
      systemsUnderTest: [
        {
          id: uuidv4(),
          name: '',
          description: '',
          isBaseline: true,
          healthCheckPath: '/actuator/health',
          appPort: 8080,
          startupTimeoutSeconds: 180,
          databaseSeedConfigs: [],
        },
      ],
    },
  })

  const { fields: systemFields, append: appendSystem, remove: removeSystem } = useFieldArray({
    control: form.control,
    name: 'systemsUnderTest',
  })

  const { fields: behaviorFields, append: appendBehavior, remove: removeBehavior } = useFieldArray({
    control: form.control,
    name: 'behaviorModels',
  })

  const { fields: operationalProfileFields, append: appendOperationalProfile, remove: removeOperationalProfile } = useFieldArray({
    control: form.control,
    name: 'operationalProfile',
  })

  // File handlers
  const handleOpenApiFileSelected = (file: UploadedFile | null) => {
    setOpenApiFile(file)
  }

  const handleComposeFileSelected = (index: number, file: UploadedFile | null) => {
    setSystemFiles((prev) => {
      const updated = [...prev]
      updated[index] = { ...updated[index], composeFile: file }
      return updated
    })
  }

  const handleSqlSeedFileSelected = (systemIndex: number, dbConfigIndex: number, file: UploadedFile | null) => {
    setSystemFiles((prev) => {
      const updated = [...prev]
      const sqlSeedFiles = [...(updated[systemIndex]?.sqlSeedFiles || [])]
      sqlSeedFiles[dbConfigIndex] = file
      updated[systemIndex] = { ...updated[systemIndex], sqlSeedFiles }
      return updated
    })
  }

  const handleAddSystem = () => {
    appendSystem({
      id: uuidv4(),
      name: '',
      description: '',
      isBaseline: false,
      healthCheckPath: '/actuator/health',
      appPort: 8080,
      startupTimeoutSeconds: 180,
      databaseSeedConfigs: [],
    })
    setSystemFiles((prev) => [...prev, { composeFile: null, sqlSeedFiles: [] }])
  }

  const handleRemoveSystem = (index: number) => {
    removeSystem(index)
    setSystemFiles((prev) => prev.filter((_, i) => i !== index))
  }

  const handleAddBehaviorModel = () => {
    appendBehavior({
      id: '',
      actor: '',
      usageProfile: 0.5,
      steps: [{ method: 'GET', path: '/', headers: [], params: [], body: '{}', save: [] }],
      thinkFrom: 1000,
      thinkTo: 3000,
    })
  }

  const handleAddOperationalProfile = () => {
    appendOperationalProfile({ load: 25, frequency: 0.2 })
  }

  // Step validation
  const validateCurrentStep = async (): Promise<boolean> => {
    switch (currentStep) {
      case 0: // Basic Info
        return form.trigger(['name', 'description'])
      case 1: // Load Test Config
        if (!openApiFile) {
          toast({
            variant: 'destructive',
            title: 'OpenAPI file required',
            description: 'Please upload an OpenAPI specification file',
          })
          return false
        }
        const opProfile = form.getValues('operationalProfile')
        const freqSum = opProfile.reduce((sum, p) => sum + Number(p.frequency), 0)
        if (Math.abs(freqSum - 1.0) > 0.001) {
          toast({
            variant: 'destructive',
            title: 'Invalid operational profile',
            description: `Frequencies must sum to 1.0 (current sum: ${freqSum.toFixed(3)})`,
          })
          return false
        }
        if (!form.getValues('generateBehaviorModels')) {
          const behaviorModels = form.getValues('behaviorModels')
          if (!behaviorModels || behaviorModels.length === 0) {
            toast({
              variant: 'destructive',
              title: 'Behavior models required',
              description: 'Either enable auto-generation or define at least one behavior model',
            })
            return false
          }
        }
        return form.trigger(['generateBehaviorModels', 'behaviorModels', 'operationalProfile'])
      case 2: // Systems Under Test
        const missingFiles = systemFiles.some((files) => !files.composeFile)
        if (missingFiles) {
          toast({
            variant: 'destructive',
            title: 'Missing files',
            description: 'Each system must have a docker-compose file',
          })
          return false
        }
        // Validate exactly one baseline
        const suts = form.getValues('systemsUnderTest')
        const baselineCount = suts.filter(s => s.isBaseline).length
        if (baselineCount === 0) {
          toast({
            variant: 'destructive',
            title: 'No baseline selected',
            description: 'Please mark exactly one system as the baseline for comparison',
          })
          return false
        }
        if (baselineCount > 1) {
          toast({
            variant: 'destructive',
            title: 'Multiple baselines selected',
            description: 'Only one system can be marked as the baseline',
          })
          return false
        }
        return form.trigger(['systemsUnderTest'])
      default:
        return true
    }
  }

  const handleNext = async () => {
    const isValid = await validateCurrentStep()
    if (isValid) {
      setCurrentStep((prev) => Math.min(prev + 1, STEPS.length - 1))
    }
  }

  const handlePrevious = () => {
    setCurrentStep((prev) => Math.max(prev - 1, 0))
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

  const onSubmit = async (data: BenchmarkFormData) => {
    try {
      // Process behavioral models
      let behaviorModels: any[] = []
      if (!data.generateBehaviorModels && data.behaviorModels && data.behaviorModels.length > 0) {
        behaviorModels = data.behaviorModels.map((model) => {
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
              headers,
              params,
              body,
              save,
              operationId: `${step.method}-${step.path}`.replace(/[^a-zA-Z0-9-]/g, '-'),
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

      const result = await createBenchmark.mutateAsync({
        data: {
          name: data.name,
          description: data.description || undefined,
          benchmarkConfig: {
            id: uuidv4(),
            openApiFileId: openApiFile!.fileId,
            behaviorModels,
            operationalProfile: data.operationalProfile,
          },
          systemsUnderTest: data.systemsUnderTest.map((system, index) => {
            const databaseSeedConfigs = system.databaseSeedConfigs
              .map((dbConfig, dbIndex) => {
                const sqlSeedFile = systemFiles[index]?.sqlSeedFiles?.[dbIndex]
                if (!sqlSeedFile) return null
                return {
                  sqlSeedFileId: sqlSeedFile.fileId,
                  dbContainerName: dbConfig.dbContainerName,
                  dbPort: dbConfig.dbPort,
                  dbName: dbConfig.dbName,
                  dbUsername: dbConfig.dbUsername,
                }
              })
              .filter((config): config is NonNullable<typeof config> => config !== null)

            return {
              id: system.id,
              name: system.name,
              description: system.description || undefined,
              isBaseline: system.isBaseline,
              dockerConfig: {
                composeFileId: systemFiles[index].composeFile!.fileId,
                healthCheckPath: system.healthCheckPath,
                appPort: system.appPort,
                startupTimeoutSeconds: system.startupTimeoutSeconds,
              },
              databaseSeedConfigs,
            }
          }),
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
                <Input id="name" {...form.register('name')} placeholder="Benchmark 1" />
                {form.formState.errors.name && (
                  <p className="text-sm text-destructive">{form.formState.errors.name.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">Description (optional)</Label>
                <Textarea
                  id="description"
                  {...form.register('description')}
                  placeholder="Comparing monolithic vs microservices performance"
                  rows={3}
                />
              </div>
            </CardContent>
          </Card>
        )

      case 1:
        return (
          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>OpenAPI Specification</CardTitle>
                <CardDescription>Upload your API specification file</CardDescription>
              </CardHeader>
              <CardContent>
                <FileSelector
                  id="openapi-file"
                  label="OpenAPI Specification File"
                  accept=".json,.yaml,.yml"
                  required
                  onFileSelected={handleOpenApiFileSelected}
                  selectedFile={openApiFile}
                  mimeTypeFilter="json"
                />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Behavior Models</CardTitle>
                <CardDescription>Define user behavior patterns for load testing</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="generateBehaviorModels"
                    {...form.register('generateBehaviorModels')}
                    className="h-4 w-4 rounded border-gray-300"
                  />
                  <Label htmlFor="generateBehaviorModels" className="cursor-pointer">
                    Auto-generate behavior models from OpenAPI specification
                  </Label>
                </div>

                {!form.watch('generateBehaviorModels') && (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <Label>Manual Behavior Models</Label>
                      <Button type="button" variant="outline" size="sm" onClick={handleAddBehaviorModel}>
                        <Plus className="h-4 w-4 mr-2" />
                        Add Model
                      </Button>
                    </div>

                    {behaviorFields.map((field, index) => (
                      <Card key={field.id} className="p-4">
                        <div className="space-y-4">
                          <div className="flex items-center justify-between mb-2">
                            <h4 className="text-sm font-semibold">Behavior Model {index + 1}</h4>
                            <Button type="button" variant="ghost" size="icon" onClick={() => removeBehavior(index)}>
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>

                          <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                              <Label htmlFor={`behavior-id-${index}`}>ID</Label>
                              <Input
                                id={`behavior-id-${index}`}
                                {...form.register(`behaviorModels.${index}.id`)}
                                placeholder="checkout-flow"
                              />
                            </div>
                            <div className="space-y-2">
                              <Label htmlFor={`behavior-actor-${index}`}>Actor</Label>
                              <Input
                                id={`behavior-actor-${index}`}
                                {...form.register(`behaviorModels.${index}.actor`)}
                                placeholder="Customer"
                              />
                            </div>
                          </div>

                          <div className="grid grid-cols-3 gap-4">
                            <div className="space-y-2">
                              <Label htmlFor={`usage-profile-${index}`}>Usage Profile (0-1)</Label>
                              <Input
                                id={`usage-profile-${index}`}
                                type="number"
                                step="0.01"
                                min="0"
                                max="1"
                                {...form.register(`behaviorModels.${index}.usageProfile`)}
                              />
                            </div>
                            <div className="space-y-2">
                              <Label htmlFor={`behavior-think-from-${index}`}>Think From (ms)</Label>
                              <Input
                                id={`behavior-think-from-${index}`}
                                type="number"
                                {...form.register(`behaviorModels.${index}.thinkFrom`)}
                              />
                            </div>
                            <div className="space-y-2">
                              <Label htmlFor={`behavior-think-to-${index}`}>Think To (ms)</Label>
                              <Input
                                id={`behavior-think-to-${index}`}
                                type="number"
                                {...form.register(`behaviorModels.${index}.thinkTo`)}
                              />
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

                    {form.formState.errors.behaviorModels && (
                      <p className="text-sm text-destructive">{form.formState.errors.behaviorModels.message}</p>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Operational Profile</CardTitle>
                <CardDescription>Define load levels and their frequency (must sum to 1.0)</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex justify-end">
                  <Button type="button" variant="outline" size="sm" onClick={handleAddOperationalProfile}>
                    <Plus className="h-4 w-4 mr-2" />
                    Add Load Level
                  </Button>
                </div>

                {operationalProfileFields.map((field, index) => (
                  <div key={field.id} className="flex items-center gap-4 p-3 rounded-lg bg-muted/50">
                    <div className="flex-1 grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor={`op-load-${index}`}>Load (users)</Label>
                        <Input
                          id={`op-load-${index}`}
                          type="number"
                          {...form.register(`operationalProfile.${index}.load`)}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor={`op-freq-${index}`}>Frequency (0-1)</Label>
                        <Input
                          id={`op-freq-${index}`}
                          type="number"
                          step="0.01"
                          min="0"
                          max="1"
                          {...form.register(`operationalProfile.${index}.frequency`)}
                        />
                      </div>
                    </div>
                    {operationalProfileFields.length > 1 && (
                      <Button type="button" variant="ghost" size="icon" onClick={() => removeOperationalProfile(index)}>
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                ))}

                {(() => {
                  const opProfile = form.watch('operationalProfile')
                  if (!opProfile || opProfile.length === 0) return null
                  const sum = opProfile.reduce((s, p) => s + (Number(p.frequency) || 0), 0)
                  const isValid = Math.abs(sum - 1.0) <= 0.001
                  return (
                    <p className={`text-xs font-medium ${isValid ? 'text-green-600' : 'text-destructive'}`}>
                      {isValid ? (
                        <span className="flex items-center gap-1"><Check className="h-3 w-3" /> Frequencies sum to 1.0</span>
                      ) : (
                        `Frequencies must sum to 1.0 (current: ${sum.toFixed(2)})`
                      )}
                    </p>
                  )
                })()}
              </CardContent>
            </Card>
          </div>
        )

      case 2:
        return (
          <Card>
            <CardHeader>
              <CardTitle>Systems Under Test</CardTitle>
              <CardDescription>Configure the systems to be tested</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {systemFields.map((field, index) => (
                <Card key={field.id} className="relative">
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle className="text-lg flex items-center gap-2">
                        System {index + 1}
                        {form.watch(`systemsUnderTest.${index}.isBaseline`) && (
                          <Badge variant="secondary">Baseline</Badge>
                        )}
                      </CardTitle>
                      {systemFields.length > 1 && (
                        <Button type="button" variant="ghost" size="icon" onClick={() => handleRemoveSystem(index)}>
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor={`system-name-${index}`}>Name *</Label>
                        <Input
                          id={`system-name-${index}`}
                          {...form.register(`systemsUnderTest.${index}.name`)}
                          placeholder="Monolithic deployment"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor={`system-description-${index}`}>Description</Label>
                        <Input
                          id={`system-description-${index}`}
                          {...form.register(`systemsUnderTest.${index}.description`)}
                          placeholder="Original monolithic version"
                        />
                      </div>
                    </div>

                    <div className="flex items-center space-x-2">
                      <input
                        type="checkbox"
                        id={`system-baseline-${index}`}
                        {...form.register(`systemsUnderTest.${index}.isBaseline`)}
                        className="h-4 w-4 rounded border-gray-300"
                      />
                      <Label htmlFor={`system-baseline-${index}`} className="cursor-pointer">
                        Use as baseline for comparison
                      </Label>
                    </div>

                    <FileSelector
                      id={`compose-file-${index}`}
                      label="Docker Compose File *"
                      accept=".yaml,.yml"
                      required
                      onFileSelected={(file) => handleComposeFileSelected(index, file)}
                      selectedFile={systemFiles[index]?.composeFile}
                      mimeTypeFilter="yaml"
                    />

                    <div className="grid grid-cols-3 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor={`health-check-${index}`}>Health Check Path</Label>
                        <Input
                          id={`health-check-${index}`}
                          {...form.register(`systemsUnderTest.${index}.healthCheckPath`)}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor={`app-port-${index}`}>App Port</Label>
                        <Input
                          id={`app-port-${index}`}
                          type="number"
                          {...form.register(`systemsUnderTest.${index}.appPort`)}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label htmlFor={`timeout-${index}`}>Startup Timeout (s)</Label>
                        <Input
                          id={`timeout-${index}`}
                          type="number"
                          {...form.register(`systemsUnderTest.${index}.startupTimeoutSeconds`)}
                        />
                      </div>
                    </div>

                    {/* Database Seed Configs (multiple for microservices) */}
                    <DatabaseSeedConfigList
                      systemIndex={index}
                      control={form.control}
                      register={form.register}
                      onFileSelected={handleSqlSeedFileSelected}
                      sqlSeedFiles={systemFiles[index]?.sqlSeedFiles || []}
                    />
                  </CardContent>
                </Card>
              ))}

              <Button type="button" variant="outline" onClick={handleAddSystem} className="w-full">
                <Plus className="h-4 w-4 mr-2" />
                Add System
              </Button>
            </CardContent>
          </Card>
        )

      case 3:
        const formData = form.getValues()
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
                      <span className="text-muted-foreground">Behavior Models:</span>{' '}
                      {formData.generateBehaviorModels ? 'Auto-generated' : `${formData.behaviorModels?.length || 0} defined`}
                    </p>
                    <p>
                      <span className="text-muted-foreground">Operational Profile:</span>{' '}
                      {formData.operationalProfile.map(p => `${p.load} users (${(Number(p.frequency) * 100).toFixed(0)}%)`).join(', ')}
                    </p>
                  </div>
                </div>

                {/* Systems Under Test Summary */}
                <div className="space-y-2">
                  <h4 className="font-semibold flex items-center gap-2">
                    <Server className="h-4 w-4" />
                    Systems Under Test ({formData.systemsUnderTest.length})
                  </h4>
                  <div className="pl-6 space-y-3">
                    {formData.systemsUnderTest.map((system, index) => (
                      <div key={system.id} className="p-3 rounded-lg bg-muted/50 text-sm">
                        <p className="font-medium flex items-center gap-2">
                          {system.name}
                          {system.isBaseline && <Badge variant="secondary" className="text-xs">Baseline</Badge>}
                        </p>
                        {system.description && (
                          <p className="text-muted-foreground text-xs">{system.description}</p>
                        )}
                        <p className="text-xs text-muted-foreground mt-1">
                          Port: {system.appPort} | Health: {system.healthCheckPath} | Compose: {systemFiles[index]?.composeFile?.filename}
                        </p>
                      </div>
                    ))}
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
          <h1 className="text-3xl font-bold">Create Load Test Benchmark</h1>
          <p className="text-muted-foreground mt-2">
            Compare performance across different deployments
          </p>
        </div>
      </div>

      <Stepper steps={STEPS} currentStep={currentStep} className="mb-8" />

      <StepContent>
        {renderStepContent()}
      </StepContent>

      <StepNavigation
        currentStep={currentStep}
        totalSteps={STEPS.length}
        onNext={handleNext}
        onPrevious={handlePrevious}
        onSubmit={handleSubmit}
        isSubmitting={createBenchmark.isPending}
        submitLabel="Create Benchmark"
      >
        <Link to="/benchmarks">
          <Button type="button" variant="outline">
            Cancel
          </Button>
        </Link>
      </StepNavigation>
    </div>
  )
}
