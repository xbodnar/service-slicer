import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { useCreateSystemUnderTest } from '@/api/generated/system-under-test-controller/system-under-test-controller'
import { type UploadedFile } from '@/hooks/useFileUpload'
import { useToast } from '@/components/ui/use-toast'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { FileSelector } from '@/components/ui/file-selector'
import { Loader2, ArrowLeft, Plus, Trash2 } from 'lucide-react'

const createSUTSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().optional(),
  healthCheckPath: z.string().min(1, 'Health check path is required'),
  appPort: z.coerce.number().min(1, 'App port must be at least 1'),
  startupTimeoutSeconds: z.coerce.number().min(1, 'Startup timeout must be at least 1'),
  databaseSeedConfigs: z.array(
    z.object({
      dbContainerName: z.string().min(1, 'Container name is required'),
      dbPort: z.coerce.number().min(1, 'DB port must be at least 1'),
      dbName: z.string().min(1, 'Database name is required'),
      dbUsername: z.string().min(1, 'Username is required'),
    })
  ),
})

type CreateSUTFormData = z.infer<typeof createSUTSchema>

export function SystemUnderTestCreatePage() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const createSystemUnderTest = useCreateSystemUnderTest()
  const [composeFile, setComposeFile] = useState<UploadedFile | null>(null)
  const [dbSeedFiles, setDbSeedFiles] = useState<(UploadedFile | null)[]>([])

  const form = useForm<CreateSUTFormData>({
    resolver: zodResolver(createSUTSchema),
    defaultValues: {
      name: '',
      description: '',
      healthCheckPath: '/actuator/health',
      appPort: 8080,
      startupTimeoutSeconds: 120,
      databaseSeedConfigs: [],
    },
  })

  const { fields: dbConfigFields, append: appendDbConfig, remove: removeDbConfig } = useFieldArray({
    control: form.control,
    name: 'databaseSeedConfigs',
  })

  const handleAddDbConfig = () => {
    appendDbConfig({
      dbContainerName: '',
      dbPort: 5432,
      dbName: '',
      dbUsername: '',
    })
    setDbSeedFiles([...dbSeedFiles, null])
  }

  const handleRemoveDbConfig = (index: number) => {
    removeDbConfig(index)
    setDbSeedFiles(dbSeedFiles.filter((_, i) => i !== index))
  }

  const onSubmit = async (formData: CreateSUTFormData) => {
    if (!composeFile) {
      toast({
        variant: 'destructive',
        title: 'Docker Compose file is required',
      })
      return
    }

    // Validate that all DB seed files are uploaded
    for (let i = 0; i < formData.databaseSeedConfigs.length; i++) {
      if (!dbSeedFiles[i]) {
        toast({
          variant: 'destructive',
          title: `SQL seed file for database ${i + 1} is required`,
        })
        return
      }
    }

    try {
      const result = await createSystemUnderTest.mutateAsync({
        data: {
          name: formData.name,
          description: formData.description || undefined,
          dockerConfig: {
            composeFileId: composeFile.fileId,
            healthCheckPath: formData.healthCheckPath,
            appPort: formData.appPort,
            startupTimeoutSeconds: formData.startupTimeoutSeconds,
          },
          databaseSeedConfigs: formData.databaseSeedConfigs.map((config, i) => ({
            sqlSeedFileId: dbSeedFiles[i]!.fileId,
            dbContainerName: config.dbContainerName,
            dbPort: config.dbPort,
            dbName: config.dbName,
            dbUsername: config.dbUsername,
          })),
        },
      })

      toast({
        title: 'System created',
        description: 'The system under test has been created successfully',
      })

      // Navigate to the detail page
      navigate(`/systems-under-test/${(result as any).systemUnderTestId}`)
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to create system',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-4">
          <Link to="/systems-under-test">
            <Button variant="outline" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <h1 className="text-2xl font-bold">New System Under Test</h1>
            <p className="text-muted-foreground mt-1">Configure a new system for benchmarking</p>
          </div>
        </div>
      </div>

      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        {/* Basic Information */}
        <Card>
          <CardHeader>
            <CardTitle>Basic Information</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Name *</Label>
              <Input id="name" {...form.register('name')} placeholder="Monolithic Architecture" />
              {form.formState.errors.name && (
                <p className="text-sm text-destructive">{form.formState.errors.name.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                {...form.register('description')}
                placeholder="Description of the system architecture..."
                rows={3}
              />
            </div>
          </CardContent>
        </Card>

        {/* Docker Configuration */}
        <Card>
          <CardHeader>
            <CardTitle>Docker Configuration</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <FileSelector
              id="compose-file"
              label="Docker Compose File"
              accept=".yml,.yaml"
              required
              onFileSelected={setComposeFile}
              selectedFile={composeFile}
              mimeTypeFilter="yaml"
            />

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="space-y-2">
                <Label htmlFor="health-check">Health Check Path *</Label>
                <Input
                  id="health-check"
                  {...form.register('healthCheckPath')}
                  placeholder="/actuator/health"
                />
                {form.formState.errors.healthCheckPath && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.healthCheckPath.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="app-port">App Port *</Label>
                <Input type="number" id="app-port" {...form.register('appPort')} />
                {form.formState.errors.appPort && (
                  <p className="text-sm text-destructive">{form.formState.errors.appPort.message}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="timeout">Startup Timeout (seconds) *</Label>
                <Input type="number" id="timeout" {...form.register('startupTimeoutSeconds')} />
                {form.formState.errors.startupTimeoutSeconds && (
                  <p className="text-sm text-destructive">
                    {form.formState.errors.startupTimeoutSeconds.message}
                  </p>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Database Configurations */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>Database Configurations</CardTitle>
              <Button type="button" variant="outline" size="sm" onClick={handleAddDbConfig}>
                <Plus className="h-4 w-4 mr-2" />
                Add Database
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {dbConfigFields.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-8">
                No database configurations. Click "Add Database" to add one.
              </p>
            ) : (
              dbConfigFields.map((field, index) => (
                <div key={field.id} className="p-4 rounded-lg bg-muted/30 border space-y-4">
                  <div className="flex items-center justify-between">
                    <Badge variant="outline">Database {index + 1}</Badge>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => handleRemoveDbConfig(index)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>

                  <FileSelector
                    id={`db-seed-${index}`}
                    label="SQL Seed File"
                    accept=".sql"
                    required
                    onFileSelected={(file) => {
                      const newFiles = [...dbSeedFiles]
                      newFiles[index] = file
                      setDbSeedFiles(newFiles)
                    }}
                    selectedFile={dbSeedFiles[index]}
                    mimeTypeFilter="sql"
                  />

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>Container Name *</Label>
                      <Input
                        {...form.register(`databaseSeedConfigs.${index}.dbContainerName`)}
                        placeholder="postgres"
                      />
                      {form.formState.errors.databaseSeedConfigs?.[index]?.dbContainerName && (
                        <p className="text-sm text-destructive">
                          {form.formState.errors.databaseSeedConfigs[index]?.dbContainerName?.message}
                        </p>
                      )}
                    </div>
                    <div className="space-y-2">
                      <Label>Port *</Label>
                      <Input
                        type="number"
                        {...form.register(`databaseSeedConfigs.${index}.dbPort`)}
                      />
                      {form.formState.errors.databaseSeedConfigs?.[index]?.dbPort && (
                        <p className="text-sm text-destructive">
                          {form.formState.errors.databaseSeedConfigs[index]?.dbPort?.message}
                        </p>
                      )}
                    </div>
                    <div className="space-y-2">
                      <Label>Database Name *</Label>
                      <Input
                        {...form.register(`databaseSeedConfigs.${index}.dbName`)}
                        placeholder="mydb"
                      />
                      {form.formState.errors.databaseSeedConfigs?.[index]?.dbName && (
                        <p className="text-sm text-destructive">
                          {form.formState.errors.databaseSeedConfigs[index]?.dbName?.message}
                        </p>
                      )}
                    </div>
                    <div className="space-y-2">
                      <Label>Username *</Label>
                      <Input
                        {...form.register(`databaseSeedConfigs.${index}.dbUsername`)}
                        placeholder="postgres"
                      />
                      {form.formState.errors.databaseSeedConfigs?.[index]?.dbUsername && (
                        <p className="text-sm text-destructive">
                          {form.formState.errors.databaseSeedConfigs[index]?.dbUsername?.message}
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>

        {/* Action Buttons */}
        <div className="flex gap-4">
          <Button type="submit" size="lg" disabled={createSystemUnderTest.isPending}>
            {createSystemUnderTest.isPending && (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            )}
            Create System
          </Button>
          <Link to="/systems-under-test">
            <Button type="button" variant="outline" size="lg">
              Cancel
            </Button>
          </Link>
        </div>
      </form>
    </div>
  )
}
