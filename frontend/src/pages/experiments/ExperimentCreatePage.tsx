import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { FileSelector } from '@/components/ui/file-selector'
import { useToast } from '@/components/ui/use-toast'
import { type UploadedFile } from '@/hooks/useFileUpload'
import { useCreateExperiment } from '@/hooks/useExperiments'
import { ArrowLeft, Loader2, Plus, Trash2 } from 'lucide-react'

const experimentSchema = z.object({
  name: z.string().min(1, 'Experiment name is required'),
  description: z.string().optional(),
  generateBehaviorModels: z.boolean().default(false),
  behaviorModels: z.array(
    z.object({
      id: z.string().min(1, 'ID is required'),
      actor: z.string().min(1, 'Actor name is required'),
      usageProfile: z.coerce.number().min(0).max(1),
      steps: z.array(
        z.object({
          method: z.string().min(1, 'Method is required'),
          path: z.string().min(1, 'Path is required'),
          headers: z.string().default('{}'),
          params: z.string().default('{}'),
          body: z.string().optional(),
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
  systemsUnderTest: z.array(
    z.object({
      name: z.string().min(1, 'System name is required'),
      description: z.string().optional(),
      healthCheckPath: z.string().default('/actuator/health'),
      appPort: z.coerce.number().default(9090),
      startupTimeoutSeconds: z.coerce.number().default(180),
    })
  ).min(1, 'At least one system under test is required'),
}).refine((data) => {
  // If not generating behavior models, require at least one behavior model to be defined
  if (!data.generateBehaviorModels && (!data.behaviorModels || data.behaviorModels.length === 0)) {
    return false
  }
  return true
}, {
  message: 'Either enable auto-generation or define at least one behavior model',
  path: ['behaviorModels'],
})

type ExperimentFormData = z.infer<typeof experimentSchema>

interface SystemFiles {
  composeFile: UploadedFile | null
  jarFile: UploadedFile | null
}

interface BehaviorModelStepsProps {
  behaviorIndex: number
  control: any
  register: any
}

function BehaviorModelSteps({ behaviorIndex, control, register }: BehaviorModelStepsProps) {
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
          onClick={() => appendStep({ method: 'GET', path: '/', headers: '{}', params: '{}', body: '' })}
        >
          <Plus className="h-4 w-4 mr-2" />
          Add Step
        </Button>
      </div>

      {stepFields.map((field, stepIndex) => (
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

            <div className="space-y-2">
              <Label htmlFor={`behavior-${behaviorIndex}-step-${stepIndex}-headers`}>Headers (JSON)</Label>
              <Textarea
                id={`behavior-${behaviorIndex}-step-${stepIndex}-headers`}
                {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.headers`)}
                placeholder='{"Content-Type": "application/json"}'
                rows={2}
                className="font-mono text-xs"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor={`behavior-${behaviorIndex}-step-${stepIndex}-params`}>Query Params (JSON)</Label>
              <Textarea
                id={`behavior-${behaviorIndex}-step-${stepIndex}-params`}
                {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.params`)}
                placeholder='{"filter": "active"}'
                rows={2}
                className="font-mono text-xs"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor={`behavior-${behaviorIndex}-step-${stepIndex}-body`}>Body (optional)</Label>
              <Textarea
                id={`behavior-${behaviorIndex}-step-${stepIndex}-body`}
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

export function ExperimentCreatePage() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const createExperiment = useCreateExperiment()

  const [openApiFile, setOpenApiFile] = useState<UploadedFile | null>(null)
  const [systemFiles, setSystemFiles] = useState<SystemFiles[]>([
    { composeFile: null, jarFile: null },
  ])

  const form = useForm<ExperimentFormData>({
    resolver: zodResolver(experimentSchema),
    defaultValues: {
      name: '',
      description: '',
      generateBehaviorModels: false,
      behaviorModels: [],
      operationalProfile: [
        {
          load: 25,
          frequency: 1.0,
        },
      ],
      systemsUnderTest: [
        {
          name: '',
          description: '',
          healthCheckPath: '/actuator/health',
          appPort: 9090,
          startupTimeoutSeconds: 180,
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

  const handleJarFileSelected = (index: number, file: UploadedFile | null) => {
    setSystemFiles((prev) => {
      const updated = [...prev]
      updated[index] = { ...updated[index], jarFile: file }
      return updated
    })
  }

  const handleAddSystem = () => {
    appendSystem({
      name: '',
      description: '',
      healthCheckPath: '/actuator/health',
      appPort: 9090,
      startupTimeoutSeconds: 180,
    })
    setSystemFiles((prev) => [...prev, { composeFile: null, jarFile: null }])
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
      steps: [{ method: 'GET', path: '/', headers: '{}', params: '{}', body: '' }],
      thinkFrom: 1000,
      thinkTo: 3000,
    })
  }

  const handleAddOperationalProfile = () => {
    appendOperationalProfile({
      load: 25,
      frequency: 0.2,
    })
  }

  const onSubmit = async (data: ExperimentFormData) => {
    if (!openApiFile) {
      toast({
        variant: 'destructive',
        title: 'OpenAPI file required',
        description: 'Please upload an OpenAPI specification file',
      })
      return
    }

    // Validate all systems have required files
    const missingFiles = systemFiles.some((files) => !files.composeFile || !files.jarFile)
    if (missingFiles) {
      toast({
        variant: 'destructive',
        title: 'Missing files',
        description: 'Each system must have both a docker-compose file and a JAR file',
      })
      return
    }

    try {
      // Process behavioral models based on generateBehaviorModels flag
      let behaviorModels: any[] = []
      if (!data.generateBehaviorModels && data.behaviorModels && data.behaviorModels.length > 0) {
        behaviorModels = data.behaviorModels.map((model) => {
          const steps = model.steps.map((step) => {
            let headers, params
            try {
              headers = JSON.parse(step.headers || '{}')
              params = JSON.parse(step.params || '{}')
            } catch (e) {
              throw new Error(`Invalid JSON for headers/params in step "${step.path}": ${e}`)
            }
            return {
              method: step.method,
              path: step.path,
              headers,
              params,
              body: step.body || undefined,
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

      // Validate operational profile frequencies sum to 1.0
      const freqSum = data.operationalProfile.reduce((sum, p) => sum + p.frequency, 0)
      if (Math.abs(freqSum - 1.0) > 0.001) {
        toast({
          variant: 'destructive',
          title: 'Invalid operational profile',
          description: `Frequencies must sum to 1.0 (current sum: ${freqSum.toFixed(3)})`,
        })
        return
      }

      const result = await createExperiment.mutateAsync({
        data: {
          name: data.name,
          description: data.description || undefined,
          loadTestConfig: {
            openApiFileId: openApiFile.fileId,
            behaviorModels,
            operationalProfile: data.operationalProfile,
            generateBehaviorModels: data.generateBehaviorModels,
          },
          systemsUnderTest: data.systemsUnderTest.map((system, index) => ({
            name: system.name,
            composeFileId: systemFiles[index].composeFile!.fileId,
            jarFileId: systemFiles[index].jarFile!.fileId,
            description: system.description || undefined,
            healthCheckPath: system.healthCheckPath,
            appPort: system.appPort,
            startupTimeoutSeconds: system.startupTimeoutSeconds,
          })),
        },
      })

      toast({
        title: 'Experiment created',
        description: 'Your experiment has been created successfully',
      })

      navigate(`/experiments/${result.experimentId}`)
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to create experiment',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/experiments">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold">Create Load Test Experiment</h1>
          <p className="text-muted-foreground mt-2">
            Compare performance across different deployments
          </p>
        </div>
      </div>

      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        {/* Basic Info */}
        <Card>
          <CardHeader>
            <CardTitle>Experiment Details</CardTitle>
            <CardDescription>Basic information about the experiment</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Name</Label>
              <Input id="name" {...form.register('name')} placeholder="Experiment 1" />
              {form.formState.errors.name && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.name.message}
                </p>
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

        {/* Load Test Configuration */}
        <Card>
          <CardHeader>
            <CardTitle>Load Test Configuration</CardTitle>
            <CardDescription>OpenAPI specification and test configuration</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <FileSelector
              id="openapi-file"
              label="OpenAPI Specification File"
              accept=".json"
              required
              onFileSelected={handleOpenApiFileSelected}
              selectedFile={openApiFile}
              mimeTypeFilter="json"
            />

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
                  <div>
                    <Label>Behavioral Models</Label>
                    <p className="text-xs text-muted-foreground mt-1">
                      Define the user behavior patterns for load testing
                    </p>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleAddBehaviorModel}
                  >
                    <Plus className="h-4 w-4 mr-2" />
                    Add Model
                  </Button>
                </div>

              {behaviorFields.map((field, index) => (
                <Card key={field.id} className="p-4">
                  <div className="space-y-4">
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="text-sm font-semibold">Behavior Model {index + 1}</h4>
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={() => removeBehavior(index)}
                      >
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
                        {form.formState.errors.behaviorModels?.[index]?.id && (
                          <p className="text-sm text-destructive">
                            {form.formState.errors.behaviorModels[index]?.id?.message}
                          </p>
                        )}
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor={`behavior-actor-${index}`}>Actor</Label>
                        <Input
                          id={`behavior-actor-${index}`}
                          {...form.register(`behaviorModels.${index}.actor`)}
                          placeholder="Customer"
                        />
                        {form.formState.errors.behaviorModels?.[index]?.actor && (
                          <p className="text-sm text-destructive">
                            {form.formState.errors.behaviorModels[index]?.actor?.message}
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor={`usage-profile-${index}`}>
                        Usage Profile (0-1)
                      </Label>
                      <Input
                        id={`usage-profile-${index}`}
                        type="number"
                        step="0.01"
                        min="0"
                        max="1"
                        {...form.register(`behaviorModels.${index}.usageProfile`)}
                      />
                    </div>

                    <BehaviorModelSteps
                      behaviorIndex={index}
                      control={form.control}
                      register={form.register}
                    />

                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor={`behavior-think-from-${index}`}>
                          Think Time From (ms)
                        </Label>
                        <Input
                          id={`behavior-think-from-${index}`}
                          type="number"
                          {...form.register(`behaviorModels.${index}.thinkFrom`)}
                        />
                      </div>

                      <div className="space-y-2">
                        <Label htmlFor={`behavior-think-to-${index}`}>
                          Think Time To (ms)
                        </Label>
                        <Input
                          id={`behavior-think-to-${index}`}
                          type="number"
                          {...form.register(`behaviorModels.${index}.thinkTo`)}
                        />
                      </div>
                    </div>
                  </div>
                </Card>
              ))}

                {form.formState.errors.behaviorModels && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.behaviorModels.message}
                  </p>
                )}
              </div>
            )}

            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <Label>Operational Profile</Label>
                  <p className="text-xs text-muted-foreground mt-1">
                    Define load levels and their frequency distribution (frequencies must sum to 1.0)
                  </p>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleAddOperationalProfile}
                >
                  <Plus className="h-4 w-4 mr-2" />
                  Add Operational Load
                </Button>
              </div>

              {operationalProfileFields.map((field, index) => (
                <Card key={field.id} className="p-4">
                  <div className="flex items-center gap-4">
                    <div className="flex-1 grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label htmlFor={`op-load-${index}`}>Load (users)</Label>
                        <Input
                          id={`op-load-${index}`}
                          type="number"
                          {...form.register(`operationalProfile.${index}.load`)}
                          placeholder="25"
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
                          placeholder="0.2"
                        />
                      </div>
                    </div>

                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => removeOperationalProfile(index)}
                      className="flex-shrink-0"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </Card>
              ))}

              {(() => {
                const opProfile = form.watch('operationalProfile')
                if (!opProfile || opProfile.length === 0) return null
                const sum = opProfile.reduce((s, p) => s + (Number(p.frequency) || 0), 0)
                const isValid = Math.abs(sum - 1.0) <= 0.001
                if (isValid) return null
                return (
                  <p className="text-xs text-destructive font-medium">
                    Frequencies must sum to 1.0 (current sum: {sum.toFixed(2)})
                  </p>
                )
              })()}

              {form.formState.errors.operationalProfile && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.operationalProfile.message}
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Systems Under Test */}
        <Card>
          <CardHeader>
            <CardTitle>Systems Under Test</CardTitle>
            <CardDescription>
              Configure the systems to be tested (e.g., monolith, microservices)
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {systemFields.map((field, index) => (
              <Card key={field.id} className="relative">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-lg">System {index + 1}</CardTitle>
                    {index > 0 && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={() => handleRemoveSystem(index)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor={`system-name-${index}`}>Name</Label>
                      <Input
                        id={`system-name-${index}`}
                        {...form.register(`systemsUnderTest.${index}.name`)}
                        placeholder="Monolithic deployment"
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor={`system-description-${index}`}>
                        Description (optional)
                      </Label>
                      <Input
                        id={`system-description-${index}`}
                        {...form.register(`systemsUnderTest.${index}.description`)}
                        placeholder="Original monolithic version"
                      />
                    </div>
                  </div>

                  <FileSelector
                    id={`compose-file-${index}`}
                    label="Docker Compose File"
                    accept=".yaml,.yml"
                    required
                    onFileSelected={(file) => handleComposeFileSelected(index, file)}
                    selectedFile={systemFiles[index]?.composeFile}
                    mimeTypeFilter="yaml"
                  />

                  <FileSelector
                    id={`jar-file-${index}`}
                    label="JAR File"
                    accept=".jar"
                    required
                    onFileSelected={(file) => handleJarFileSelected(index, file)}
                    selectedFile={systemFiles[index]?.jarFile}
                    mimeTypeFilter="jar"
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
                </CardContent>
              </Card>
            ))}

            <Button type="button" variant="outline" onClick={handleAddSystem} className="w-full">
              <Plus className="h-4 w-4 mr-2" />
              Add System
            </Button>
          </CardContent>
        </Card>

        <div className="flex gap-4">
          <Button
            type="submit"
            disabled={
              createExperiment.isPending ||
              !openApiFile ||
              (() => {
                const opProfile = form.watch('operationalProfile')
                if (!opProfile || opProfile.length === 0) return true // Must have at least one
                const sum = opProfile.reduce((s, p) => s + (Number(p.frequency) || 0), 0)
                return Math.abs(sum - 1.0) > 0.001
              })()
            }
          >
            {createExperiment.isPending && (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            )}
            Create Experiment
          </Button>
          <Link to="/experiments">
            <Button type="button" variant="outline">
              Cancel
            </Button>
          </Link>
        </div>
      </form>
    </div>
  )
}
