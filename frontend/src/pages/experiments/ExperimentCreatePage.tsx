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
import { FileInput } from '@/components/ui/file-input'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/components/ui/use-toast'
import { useFileUpload, type UploadedFile } from '@/hooks/useFileUpload'
import { useCreateExperiment } from '@/hooks/useExperiments'
import { ArrowLeft, Loader2, Plus, Trash2, FileCode, FileArchive, Package, CheckCircle2 } from 'lucide-react'

const experimentSchema = z.object({
  name: z.string().min(1, 'Experiment name is required'),
  description: z.string().optional(),
  behaviorModels: z.array(
    z.object({
      id: z.string().min(1, 'ID is required'),
      actor: z.string().min(1, 'Actor name is required'),
      behaviorProbability: z.coerce.number().min(0).max(1),
      steps: z.string().min(1, 'At least one step is required'),
      thinkFrom: z.coerce.number().min(0),
      thinkTo: z.coerce.number().min(0),
    })
  ).optional(),
  operationalProfile: z.object({
    loads: z.string().optional(),
    freq: z.string().optional(),
  }).optional(),
  systemsUnderTest: z.array(
    z.object({
      name: z.string().min(1, 'System name is required'),
      description: z.string().optional(),
      healthCheckPath: z.string().default('/actuator/health'),
      appPort: z.coerce.number().default(9090),
      startupTimeoutSeconds: z.coerce.number().default(180),
    })
  ).min(1, 'At least one system under test is required'),
})

type ExperimentFormData = z.infer<typeof experimentSchema>

interface SystemFiles {
  composeFile: UploadedFile | null
  jarFile: UploadedFile | null
}

export function ExperimentCreatePage() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const { uploadFile, isUploading } = useFileUpload()
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
      behaviorModels: [],
      operationalProfile: {
        loads: '',
        freq: '',
      },
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

  const handleOpenApiUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    const result = await uploadFile(file)
    if (result) {
      setOpenApiFile(result)
    }
  }

  const handleComposeUpload = async (
    index: number,
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = event.target.files?.[0]
    if (!file) return

    const result = await uploadFile(file)
    if (result) {
      setSystemFiles((prev) => {
        const updated = [...prev]
        updated[index] = { ...updated[index], composeFile: result }
        return updated
      })
    }
  }

  const handleJarUpload = async (
    index: number,
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = event.target.files?.[0]
    if (!file) return

    const result = await uploadFile(file)
    if (result) {
      setSystemFiles((prev) => {
        const updated = [...prev]
        updated[index] = { ...updated[index], jarFile: result }
        return updated
      })
    }
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
      behaviorProbability: 0.5,
      steps: '',
      thinkFrom: 1000,
      thinkTo: 3000,
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
      // Process behavioral models if provided
      let behaviorModels: any[] | undefined
      if (data.behaviorModels && data.behaviorModels.length > 0) {
        behaviorModels = data.behaviorModels.map((model) => ({
          id: model.id,
          actor: model.actor,
          behaviorProbability: model.behaviorProbability,
          steps: model.steps.split(',').map((s) => s.trim()),
          thinkFrom: model.thinkFrom,
          thinkTo: model.thinkTo,
        }))
      }

      // Process operational profile if provided
      let operationalProfile: any | null = null
      if (data.operationalProfile?.loads && data.operationalProfile?.freq) {
        const loads = data.operationalProfile.loads
          .split(',')
          .map((l) => parseInt(l.trim(), 10))
          .filter((l) => !isNaN(l))

        const freq = data.operationalProfile.freq
          .split(',')
          .map((f) => parseFloat(f.trim()))
          .filter((f) => !isNaN(f))

        if (loads.length > 0 && freq.length > 0) {
          if (loads.length !== freq.length) {
            toast({
              variant: 'destructive',
              title: 'Invalid operational profile',
              description: 'Loads and frequencies must have the same number of elements',
            })
            return
          }

          const freqSum = freq.reduce((sum, f) => sum + f, 0)
          if (Math.abs(freqSum - 1.0) > 0.001) {
            toast({
              variant: 'destructive',
              title: 'Invalid operational profile',
              description: `Frequencies must sum to 1.0 (current sum: ${freqSum.toFixed(3)})`,
            })
            return
          }

          operationalProfile = { loads, freq }
        }
      }

      const result = await createExperiment.mutateAsync({
        data: {
          name: data.name,
          description: data.description || undefined,
          loadTestConfig: {
            openApiFileId: openApiFile.fileId,
            behaviorModels,
            operationalProfile,
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
            <div className="space-y-2">
              <Label htmlFor="openapi-file">OpenAPI Specification File</Label>
              <FileInput
                id="openapi-file"
                accept=".yaml,.yml,.json"
                onChange={handleOpenApiUpload}
                disabled={isUploading}
              />
              {openApiFile && (
                <div className="flex items-center gap-2 p-3 rounded-lg bg-muted/50 border">
                  <FileCode className="h-4 w-4 text-primary flex-shrink-0" />
                  <span className="text-sm font-medium flex-1 truncate">{openApiFile.filename}</span>
                  <Badge variant="secondary" className="text-xs">
                    {(openApiFile.size / 1024).toFixed(2)} KB
                  </Badge>
                  <CheckCircle2 className="h-4 w-4 text-green-500 flex-shrink-0" />
                </div>
              )}
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <Label>Behavioral Models (optional)</Label>
                  <p className="text-xs text-muted-foreground mt-1">
                    If left empty, AI will generate behavioral models based on the OpenAPI specification
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
                      <Label htmlFor={`behavior-probability-${index}`}>
                        Behavior Probability (0-1)
                      </Label>
                      <Input
                        id={`behavior-probability-${index}`}
                        type="number"
                        step="0.01"
                        min="0"
                        max="1"
                        {...form.register(`behaviorModels.${index}.behaviorProbability`)}
                      />
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor={`behavior-steps-${index}`}>
                        Steps (comma-separated operation IDs)
                      </Label>
                      <Textarea
                        id={`behavior-steps-${index}`}
                        {...form.register(`behaviorModels.${index}.steps`)}
                        placeholder="createOrder,addPayment,confirmOrder"
                        rows={2}
                      />
                      <p className="text-xs text-muted-foreground">
                        Enter operation IDs from your OpenAPI spec, separated by commas
                      </p>
                    </div>

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
            </div>

            <div className="space-y-4">
              <div>
                <Label>Operational Profile (optional)</Label>
                <p className="text-xs text-muted-foreground mt-1">
                  If left empty, AI will generate an operational profile based on the OpenAPI specification
                </p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="op-loads">Loads (comma-separated)</Label>
                  <Input
                    id="op-loads"
                    {...form.register('operationalProfile.loads')}
                    placeholder="25,50,100,150,200"
                  />
                  <p className="text-xs text-muted-foreground">
                    Example: 25,50,100,150,200
                  </p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="op-freq">Frequencies (comma-separated, must sum to 1)</Label>
                  <Input
                    id="op-freq"
                    {...form.register('operationalProfile.freq')}
                    placeholder="0.1,0.2,0.3,0.3,0.1"
                  />
                  <p className="text-xs text-muted-foreground">
                    Example: 0.1,0.2,0.3,0.3,0.1 (must sum to 1.0)
                  </p>
                </div>
              </div>
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

                  <div className="space-y-2">
                    <Label htmlFor={`compose-file-${index}`}>Docker Compose File</Label>
                    <FileInput
                      id={`compose-file-${index}`}
                      accept=".yaml,.yml"
                      onChange={(e) => handleComposeUpload(index, e)}
                      disabled={isUploading}
                    />
                    {systemFiles[index]?.composeFile && (
                      <div className="flex items-center gap-2 p-3 rounded-lg bg-muted/50 border">
                        <FileArchive className="h-4 w-4 text-primary flex-shrink-0" />
                        <span className="text-sm font-medium flex-1 truncate">
                          {systemFiles[index].composeFile!.filename}
                        </span>
                        <CheckCircle2 className="h-4 w-4 text-green-500 flex-shrink-0" />
                      </div>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor={`jar-file-${index}`}>JAR File</Label>
                    <FileInput
                      id={`jar-file-${index}`}
                      accept=".jar"
                      onChange={(e) => handleJarUpload(index, e)}
                      disabled={isUploading}
                    />
                    {systemFiles[index]?.jarFile && (
                      <div className="flex items-center gap-2 p-3 rounded-lg bg-muted/50 border">
                        <Package className="h-4 w-4 text-primary flex-shrink-0" />
                        <span className="text-sm font-medium flex-1 truncate">
                          {systemFiles[index].jarFile!.filename}
                        </span>
                        <CheckCircle2 className="h-4 w-4 text-green-500 flex-shrink-0" />
                      </div>
                    )}
                  </div>

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
          <Button type="submit" disabled={createExperiment.isPending || !openApiFile}>
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

        {isUploading && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            Uploading file...
          </div>
        )}
      </form>
    </div>
  )
}
