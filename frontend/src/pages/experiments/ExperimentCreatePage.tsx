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
import { useToast } from '@/components/ui/use-toast'
import { useFileUpload, type UploadedFile } from '@/hooks/useFileUpload'
import { useCreateExperiment } from '@/hooks/useExperiments'
import { ArrowLeft, Loader2, Plus, Trash2 } from 'lucide-react'

const experimentSchema = z.object({
  name: z.string().min(1, 'Experiment name is required'),
  description: z.string().optional(),
  loadTestConfigName: z.string().min(1, 'Config name is required'),
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
      loadTestConfigName: '',
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

  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: 'systemsUnderTest',
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
    append({
      name: '',
      description: '',
      healthCheckPath: '/actuator/health',
      appPort: 9090,
      startupTimeoutSeconds: 180,
    })
    setSystemFiles((prev) => [...prev, { composeFile: null, jarFile: null }])
  }

  const handleRemoveSystem = (index: number) => {
    remove(index)
    setSystemFiles((prev) => prev.filter((_, i) => i !== index))
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
      const result = await createExperiment.mutateAsync({
        name: data.name,
        description: data.description || null,
        loadTestConfig: {
          openApiFileId: openApiFile.fileId,
          name: data.loadTestConfigName,
          behaviorModels: [],
          operationalProfile: null,
        },
        systemsUnderTest: data.systemsUnderTest.map((system, index) => ({
          name: system.name,
          composeFileId: systemFiles[index].composeFile!.fileId,
          jarFileId: systemFiles[index].jarFile!.fileId,
          description: system.description || null,
          healthCheckPath: system.healthCheckPath,
          appPort: system.appPort,
          startupTimeoutSeconds: system.startupTimeoutSeconds,
        })),
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
              <Label htmlFor="loadTestConfigName">Configuration Name</Label>
              <Input
                id="loadTestConfigName"
                {...form.register('loadTestConfigName')}
                placeholder="Default Load Test"
              />
              {form.formState.errors.loadTestConfigName && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.loadTestConfigName.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="openapi-file">OpenAPI Specification File</Label>
              <div className="flex items-center gap-4">
                <Input
                  id="openapi-file"
                  type="file"
                  accept=".yaml,.yml,.json"
                  onChange={handleOpenApiUpload}
                  disabled={isUploading}
                />
                {openApiFile && (
                  <span className="text-sm text-muted-foreground">{openApiFile.filename}</span>
                )}
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
            {fields.map((field, index) => (
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
                    <div className="flex items-center gap-4">
                      <Input
                        id={`compose-file-${index}`}
                        type="file"
                        accept=".yaml,.yml"
                        onChange={(e) => handleComposeUpload(index, e)}
                        disabled={isUploading}
                      />
                      {systemFiles[index]?.composeFile && (
                        <span className="text-sm text-muted-foreground">
                          {systemFiles[index].composeFile!.filename}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor={`jar-file-${index}`}>JAR File</Label>
                    <div className="flex items-center gap-4">
                      <Input
                        id={`jar-file-${index}`}
                        type="file"
                        accept=".jar"
                        onChange={(e) => handleJarUpload(index, e)}
                        disabled={isUploading}
                      />
                      {systemFiles[index]?.jarFile && (
                        <span className="text-sm text-muted-foreground">
                          {systemFiles[index].jarFile!.filename}
                        </span>
                      )}
                    </div>
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
