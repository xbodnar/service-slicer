import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { useExperiment } from '@/hooks/useExperiments'
import { useUpdateLoadTestConfig } from '@/api/generated/load-test-experiments-controller/load-test-experiments-controller'
import {
  useAddSystemUnderTest,
  useUpdateSystemUnderTest,
  useDeleteSystemUnderTest,
} from '@/api/generated/system-under-test-controller/system-under-test-controller'
import { useGenerateBehaviorModels } from '@/api/generated/load-test-experiments-controller/load-test-experiments-controller'
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
import { cn } from '@/lib/utils'
import {
  Loader2,
  ArrowLeft,
  FileCode,
  Server,
  Package,
  Activity,
  FileArchive,
  Users,
  Pencil,
  X,
  Plus,
  Trash2,
} from 'lucide-react'

const loadTestConfigSchema = z.object({
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
  ).optional(),
})

const systemUnderTestSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().optional(),
  healthCheckPath: z.string().min(1, 'Health check path is required'),
  appPort: z.coerce.number().min(1).max(65535, 'Port must be between 1 and 65535'),
  startupTimeoutSeconds: z.coerce.number().min(1, 'Timeout must be at least 1 second'),
})

type LoadTestConfigFormData = z.infer<typeof loadTestConfigSchema>
type SystemUnderTestFormData = z.infer<typeof systemUnderTestSchema>

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

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`
}

export function ExperimentDetailPage() {
  const { experimentId } = useParams<{ experimentId: string }>()
  const { data, isLoading, error, refetch } = useExperiment(experimentId!)
  const { toast } = useToast()
  const updateLoadTestConfig = useUpdateLoadTestConfig()
  const generateBehaviorModels = useGenerateBehaviorModels()
  const addSut = useAddSystemUnderTest()
  const updateSut = useUpdateSystemUnderTest()
  const deleteSut = useDeleteSystemUnderTest()

  const [isEditing, setIsEditing] = useState(false)
  const [openApiFile, setOpenApiFile] = useState<UploadedFile | null>(null)

  // SUT management state
  const [isAddingSut, setIsAddingSut] = useState(false)
  const [editingSutId, setEditingSutId] = useState<string | null>(null)
  const [sutComposeFile, setSutComposeFile] = useState<UploadedFile | null>(null)
  const [sutJarFile, setSutJarFile] = useState<UploadedFile | null>(null)

  const form = useForm<LoadTestConfigFormData>({
    resolver: zodResolver(loadTestConfigSchema),
    defaultValues: {
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

  const sutForm = useForm<SystemUnderTestFormData>({
    resolver: zodResolver(systemUnderTestSchema),
    defaultValues: {
      name: '',
      description: '',
      healthCheckPath: '/actuator/health',
      appPort: 8080,
      startupTimeoutSeconds: 60,
    },
  })

  const handleStartEdit = () => {
    if (!data) return

    // Pre-populate form with existing data
    const behaviorModels = data.loadTestConfig.behaviorModels.map((model: any) => ({
      id: model.id,
      actor: model.actor,
      usageProfile: model.usageProfile,
      steps: model.steps.map((step: any) => ({
        method: step.method,
        path: step.path,
        headers: JSON.stringify(step.headers),
        params: JSON.stringify(step.params),
        body: step.body || '',
      })),
      thinkFrom: model.thinkFrom,
      thinkTo: model.thinkTo,
    }))

    form.reset({
      behaviorModels: behaviorModels.length > 0 ? behaviorModels : [],
      operationalProfile: data.loadTestConfig.operationalProfile || [],
    })

    setIsEditing(true)
  }

  const handleCancelEdit = () => {
    setIsEditing(false)
    setOpenApiFile(null)
    form.reset()
  }

  const handleOpenApiFileSelected = (file: UploadedFile | null) => {
    setOpenApiFile(file)
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

  const onSubmit = async (formData: LoadTestConfigFormData) => {
    if (!data) return

    // Use existing OpenAPI file if no new one was uploaded
    const openApiFileId = openApiFile?.fileId || data.loadTestConfig.openApiFile.fileId

    try {
      // Process behavioral models
      let behaviorModels: any[] = []
      if (formData.behaviorModels && formData.behaviorModels.length > 0) {
        behaviorModels = formData.behaviorModels.map((model) => {
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

      // Process operational profile
      let operationalProfile: any = undefined
      if (formData.operationalProfile && formData.operationalProfile.length > 0) {
        const freqSum = formData.operationalProfile.reduce((sum, p) => sum + p.frequency, 0)
        if (Math.abs(freqSum - 1.0) > 0.001) {
          toast({
            variant: 'destructive',
            title: 'Invalid operational profile',
            description: `Frequencies must sum to 1.0 (current sum: ${freqSum.toFixed(3)})`,
          })
          return
        }

        operationalProfile = formData.operationalProfile
      }

      await updateLoadTestConfig.mutateAsync({
        experimentId: experimentId!,
        data: {
          openApiFileId,
          behaviorModels,
          operationalProfile,
        },
      })

      toast({
        title: 'Configuration updated',
        description: 'Load test configuration has been updated successfully',
      })

      setIsEditing(false)
      setOpenApiFile(null)
      refetch()
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to update configuration',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  // SUT handlers
  const handleStartAddSut = () => {
    sutForm.reset()
    setSutComposeFile(null)
    setSutJarFile(null)
    setIsAddingSut(true)
  }

  const handleStartEditSut = (sut: any) => {
    sutForm.reset({
      name: sut.name,
      description: sut.description || '',
      healthCheckPath: sut.healthCheckPath,
      appPort: sut.appPort,
      startupTimeoutSeconds: sut.startupTimeoutSeconds,
    })
    setSutComposeFile(null)
    setSutJarFile(null)
    setEditingSutId(sut.systemUnderTestId)
  }

  const handleCancelSut = () => {
    setIsAddingSut(false)
    setEditingSutId(null)
    setSutComposeFile(null)
    setSutJarFile(null)
    sutForm.reset()
  }

  const handleComposeFileSelected = (file: UploadedFile | null) => {
    setSutComposeFile(file)
  }

  const handleJarFileSelected = (file: UploadedFile | null) => {
    setSutJarFile(file)
  }

  const onSubmitSut = async (formData: SystemUnderTestFormData) => {
    if (!data) return

    try {
      if (editingSutId) {
        // Update existing SUT
        const existingSut = data.systemsUnderTest.find((s: any) => s.systemUnderTestId === editingSutId)
        if (!existingSut) return

        await updateSut.mutateAsync({
          experimentId: experimentId!,
          sutId: editingSutId,
          data: {
            name: formData.name,
            description: formData.description,
            healthCheckPath: formData.healthCheckPath,
            appPort: formData.appPort,
            startupTimeoutSeconds: formData.startupTimeoutSeconds,
            composeFileId: sutComposeFile?.fileId || existingSut.composeFile.fileId,
            jarFileId: sutJarFile?.fileId || existingSut.jarFile.fileId,
          },
        })

        toast({
          title: 'SUT updated',
          description: 'System under test has been updated successfully',
        })
      } else {
        // Add new SUT
        if (!sutComposeFile || !sutJarFile) {
          toast({
            variant: 'destructive',
            title: 'Missing files',
            description: 'Please upload both Docker Compose and JAR files',
          })
          return
        }

        await addSut.mutateAsync({
          experimentId: experimentId!,
          data: {
            name: formData.name,
            description: formData.description,
            healthCheckPath: formData.healthCheckPath,
            appPort: formData.appPort,
            startupTimeoutSeconds: formData.startupTimeoutSeconds,
            composeFileId: sutComposeFile.fileId,
            jarFileId: sutJarFile.fileId,
          },
        })

        toast({
          title: 'SUT added',
          description: 'System under test has been added successfully',
        })
      }

      handleCancelSut()
      refetch()
    } catch (error) {
      toast({
        variant: 'destructive',
        title: editingSutId ? 'Failed to update SUT' : 'Failed to add SUT',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const handleDeleteSut = async (sutId: string) => {
    if (!confirm('Are you sure you want to delete this system under test?')) return

    try {
      await deleteSut.mutateAsync({
        experimentId: experimentId!,
        sutId,
      })

      toast({
        title: 'SUT deleted',
        description: 'System under test has been deleted successfully',
      })

      refetch()
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to delete SUT',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const handleGenerateBehaviorModels = async () => {
    try {
      await generateBehaviorModels.mutateAsync({
        experimentId: experimentId!,
      })

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
          Error loading experiment: {(error as Error)?.message || 'Unknown error'}
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start gap-4">
        <Link to="/experiments">
          <Button variant="outline" size="icon" className="h-9 w-9 flex-shrink-0 mt-1">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div className="flex-1 min-w-0">
          <h1 className="text-2xl font-bold tracking-tight">{data.name}</h1>
          {data.description && (
            <p className="text-muted-foreground mt-1.5">{data.description}</p>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Load Test Configuration */}
        <Card className="border-2 transition-shadow hover:shadow-md">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-primary/10">
                  <Activity className="h-5 w-5 text-primary" />
                </div>
                <CardTitle>Load Test Configuration</CardTitle>
              </div>
              {!isEditing && (
                <Button variant="outline" size="sm" onClick={handleStartEdit}>
                  <Pencil className="h-4 w-4 mr-2" />
                  Edit
                </Button>
              )}
            </div>
          </CardHeader>
          <CardContent className="space-y-5">
            {isEditing ? (
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                {/* OpenAPI File */}
                <div className="space-y-2">
                  <FileSelector
                    id="openapi-file"
                    label="OpenAPI Specification File (optional - leave empty to keep current)"
                    accept=".json"
                    onFileSelected={handleOpenApiFileSelected}
                    selectedFile={openApiFile}
                    mimeTypeFilter="json"
                  />
                  {!openApiFile && (
                    <div className="flex items-center gap-2 p-3 rounded-lg bg-muted/50 border">
                      <FileCode className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                      <span className="text-sm text-muted-foreground">
                        Current: {data.loadTestConfig.openApiFile.filename}
                      </span>
                    </div>
                  )}
                </div>

                {/* Behavioral Models */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <Label>Behavioral Models</Label>
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
                      <div className="space-y-3">
                        <div className="flex items-end gap-2">
                          <div className="flex-1 grid grid-cols-2 gap-3">
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
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            onClick={() => removeBehavior(index)}
                            className="flex-shrink-0"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>

                        <div className="grid grid-cols-3 gap-3">
                          <div className="space-y-2">
                            <Label htmlFor={`usage-profile-${index}`}>Usage Profile</Label>
                            <Input
                              id={`usage-profile-${index}`}
                              type="number"
                              step="0.01"
                              min="0"
                              max="1"
                              {...form.register(`behaviorModels.${index}.usageProfile`)}
                              placeholder="0.5"
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor={`behavior-think-from-${index}`}>Think From (ms)</Label>
                            <Input
                              id={`behavior-think-from-${index}`}
                              type="number"
                              {...form.register(`behaviorModels.${index}.thinkFrom`)}
                              placeholder="1000"
                            />
                          </div>
                          <div className="space-y-2">
                            <Label htmlFor={`behavior-think-to-${index}`}>Think To (ms)</Label>
                            <Input
                              id={`behavior-think-to-${index}`}
                              type="number"
                              {...form.register(`behaviorModels.${index}.thinkTo`)}
                              placeholder="3000"
                            />
                          </div>
                        </div>

                        <BehaviorModelSteps
                          behaviorIndex={index}
                          control={form.control}
                          register={form.register}
                        />
                      </div>
                    </Card>
                  ))}
                </div>

                {/* Operational Profile */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <Label>Operational Profile</Label>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={handleAddOperationalProfile}
                    >
                      <Plus className="h-4 w-4 mr-2" />
                      Add Load & Frequency
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

                  {operationalProfileFields.length > 0 && (() => {
                    const opProfile = form.watch('operationalProfile')
                    const sum = opProfile?.reduce((s, p) => s + (Number(p.frequency) || 0), 0) || 0
                    const isValid = Math.abs(sum - 1.0) <= 0.001
                    if (isValid) return null
                    return (
                      <p className="text-xs text-destructive font-medium">
                        Frequencies must sum to 1.0 (current sum: {sum.toFixed(2)})
                      </p>
                    )
                  })()}
                </div>

                {/* Action Buttons */}
                <div className="flex gap-2 pt-4">
                  <Button
                    type="submit"
                    disabled={
                      updateLoadTestConfig.isPending ||
                      (() => {
                        const opProfile = form.watch('operationalProfile')
                        if (!opProfile || opProfile.length === 0) return false
                        const sum = opProfile.reduce((s, p) => s + (Number(p.frequency) || 0), 0)
                        return Math.abs(sum - 1.0) > 0.001
                      })()
                    }
                  >
                    {updateLoadTestConfig.isPending && (
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    )}
                    Save Changes
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={handleCancelEdit}
                    disabled={updateLoadTestConfig.isPending}
                  >
                    <X className="h-4 w-4 mr-2" />
                    Cancel
                  </Button>
                </div>
              </form>
            ) : (
              <>
            {/* OpenAPI File */}
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                <FileCode className="h-4 w-4" />
                <span>OpenAPI Specification</span>
              </div>
              <div className="flex items-center gap-2 pl-6">
                <span className="font-medium">{data.loadTestConfig.openApiFile.filename}</span>
                <Badge variant="secondary" className="text-xs">
                  {formatFileSize(data.loadTestConfig.openApiFile.fileSize)}
                </Badge>
              </div>
            </div>

            {/* Generate Behavior Models Button */}
            <div className="pt-2">
              <Button
                variant="default"
                size="sm"
                onClick={handleGenerateBehaviorModels}
                disabled={generateBehaviorModels.isPending}
              >
                {generateBehaviorModels.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    {data.loadTestConfig.behaviorModels.length > 0 ? 'Regenerating...' : 'Generating...'}
                  </>
                ) : (
                  <>
                    <Activity className="h-4 w-4 mr-2" />
                    {data.loadTestConfig.behaviorModels.length > 0 ? 'Regenerate Behavior Models' : 'Generate Behavior Models'}
                  </>
                )}
              </Button>
            </div>

            {/* Behavior Models */}
            {data.loadTestConfig.behaviorModels.length > 0 && (
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                    <Users className="h-4 w-4" />
                    <span>Behavior Models</span>
                  </div>
                  <Badge variant="outline">{data.loadTestConfig.behaviorModels.length}</Badge>
                </div>
                <div className="space-y-2 pl-6">
                  {data.loadTestConfig.behaviorModels.map((model) => (
                    <div key={model.id} className="p-3 rounded-lg bg-muted/50 space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-sm">{model.id}</span>
                        <span className="text-xs text-muted-foreground">•</span>
                        <span className="text-sm text-muted-foreground">{model.actor}</span>
                        <Badge variant="secondary" className="ml-auto">
                          {(model.usageProfile * 100).toFixed(0)}%
                        </Badge>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        {model.steps.map((step: any) => `${step.method} ${step.path}`).join(' → ')} • Think: {model.thinkFrom}-{model.thinkTo}ms
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Operational Profile */}
            {data.loadTestConfig.operationalProfile && data.loadTestConfig.operationalProfile.length > 0 && (
              <div className="space-y-2">
                <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                  <Activity className="h-4 w-4" />
                  <span>Operational Profile</span>
                </div>
                <div className="pl-6 text-sm space-y-1">
                  <p><span className="text-muted-foreground">Loads:</span> {data.loadTestConfig.operationalProfile.map(p => p.load).join(', ')}</p>
                  <p>
                    <span className="text-muted-foreground">Frequencies:</span>{' '}
                    {data.loadTestConfig.operationalProfile
                      .map((p) => (p.frequency * 100).toFixed(0) + '%')
                      .join(', ')}
                  </p>
                </div>
              </div>
            )}
            </>
            )}
          </CardContent>
        </Card>

        {/* Systems Under Test */}
        <Card className="border-2 transition-shadow hover:shadow-md">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="p-2 rounded-lg bg-primary/10">
                  <Server className="h-5 w-5 text-primary" />
                </div>
                <CardTitle className="flex items-center gap-2">
                  Systems Under Test
                  <Badge variant="outline">{data.systemsUnderTest.length}</Badge>
                </CardTitle>
              </div>
              {!isAddingSut && !editingSutId && (
                <Button variant="outline" size="sm" onClick={handleStartAddSut}>
                  <Plus className="h-4 w-4 mr-2" />
                  Add SUT
                </Button>
              )}
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Add/Edit SUT Form */}
            {(isAddingSut || editingSutId) && (
              <form onSubmit={sutForm.handleSubmit(onSubmitSut)} className="space-y-4 p-4 rounded-lg border-2 border-primary/20 bg-primary/5">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-sm font-semibold">
                    {editingSutId ? 'Edit System Under Test' : 'Add New System Under Test'}
                  </h4>
                </div>

                <div className="grid grid-cols-1 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="sut-name">Name *</Label>
                    <Input
                      id="sut-name"
                      {...sutForm.register('name')}
                      placeholder="Monolithic System"
                    />
                    {sutForm.formState.errors.name && (
                      <p className="text-xs text-destructive">{sutForm.formState.errors.name.message}</p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="sut-description">Description</Label>
                    <Textarea
                      id="sut-description"
                      {...sutForm.register('description')}
                      placeholder="Original monolithic application"
                      rows={2}
                    />
                  </div>

                  <FileSelector
                    id="sut-compose"
                    label={`Docker Compose File ${editingSutId ? '(leave empty to keep current)' : '*'}`}
                    accept=".yml,.yaml"
                    required={!editingSutId}
                    onFileSelected={handleComposeFileSelected}
                    selectedFile={sutComposeFile}
                    mimeTypeFilter="yaml"
                  />

                  <FileSelector
                    id="sut-jar"
                    label={`JAR File ${editingSutId ? '(leave empty to keep current)' : '*'}`}
                    accept=".jar"
                    required={!editingSutId}
                    onFileSelected={handleJarFileSelected}
                    selectedFile={sutJarFile}
                    mimeTypeFilter="jar"
                  />

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="sut-health">Health Check Path *</Label>
                      <Input
                        id="sut-health"
                        {...sutForm.register('healthCheckPath')}
                        placeholder="/actuator/health"
                      />
                      {sutForm.formState.errors.healthCheckPath && (
                        <p className="text-xs text-destructive">{sutForm.formState.errors.healthCheckPath.message}</p>
                      )}
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="sut-port">App Port *</Label>
                      <Input
                        id="sut-port"
                        type="number"
                        {...sutForm.register('appPort')}
                        placeholder="8080"
                      />
                      {sutForm.formState.errors.appPort && (
                        <p className="text-xs text-destructive">{sutForm.formState.errors.appPort.message}</p>
                      )}
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="sut-timeout">Startup Timeout (seconds) *</Label>
                    <Input
                      id="sut-timeout"
                      type="number"
                      {...sutForm.register('startupTimeoutSeconds')}
                      placeholder="60"
                    />
                    {sutForm.formState.errors.startupTimeoutSeconds && (
                      <p className="text-xs text-destructive">{sutForm.formState.errors.startupTimeoutSeconds.message}</p>
                    )}
                  </div>
                </div>

                <div className="flex gap-2">
                  <Button
                    type="submit"
                    disabled={addSut.isPending || updateSut.isPending}
                  >
                    {(addSut.isPending || updateSut.isPending) && (
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    )}
                    {editingSutId ? 'Update SUT' : 'Add SUT'}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={handleCancelSut}
                    disabled={addSut.isPending || updateSut.isPending}
                  >
                    <X className="h-4 w-4 mr-2" />
                    Cancel
                  </Button>
                </div>
              </form>
            )}

            {/* Existing SUTs List */}
            {data.systemsUnderTest.map((system, idx) => (
              <div
                key={system.systemUnderTestId}
                className={cn(
                  'space-y-3 p-4 rounded-lg bg-muted/50 transition-colors hover:bg-muted',
                  idx > 0 && 'mt-4'
                )}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <h4 className="font-semibold">{system.name}</h4>
                    {system.description && (
                      <p className="text-sm text-muted-foreground mt-0.5">
                        {system.description}
                      </p>
                    )}
                  </div>
                  {!isAddingSut && !editingSutId && (
                    <div className="flex gap-2 ml-4">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 hover:bg-primary/10 hover:text-primary transition-colors"
                        onClick={() => handleStartEditSut(system)}
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 text-muted-foreground hover:bg-destructive/10 hover:text-destructive transition-colors"
                        onClick={() => handleDeleteSut(system.systemUnderTestId)}
                        disabled={deleteSut.isPending}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  )}
                </div>

                <div className="grid grid-cols-1 gap-2.5">
                  {/* Docker Compose */}
                  <div className="flex items-start gap-2">
                    <FileArchive className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="text-xs text-muted-foreground">Docker Compose</p>
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono text-sm truncate">{system.composeFile.filename}</span>
                        <Badge variant="secondary" className="text-xs">
                          {formatFileSize(system.composeFile.fileSize)}
                        </Badge>
                      </div>
                    </div>
                  </div>

                  {/* JAR File */}
                  <div className="flex items-start gap-2">
                    <Package className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="text-xs text-muted-foreground">Application JAR</p>
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono text-sm truncate">{system.jarFile.filename}</span>
                        <Badge variant="secondary" className="text-xs">
                          {formatFileSize(system.jarFile.fileSize)}
                        </Badge>
                      </div>
                    </div>
                  </div>

                  {/* Health & Port */}
                  <div className="grid grid-cols-2 gap-2 pt-2 border-t">
                    <div>
                      <p className="text-xs text-muted-foreground">Health Check</p>
                      <p className="font-mono text-sm">{system.healthCheckPath}</p>
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground">Port</p>
                      <p className="text-sm font-medium">{system.appPort}</p>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
