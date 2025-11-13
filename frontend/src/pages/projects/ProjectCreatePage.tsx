import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { FileSelector } from '@/components/ui/file-selector'
import { useToast } from '@/components/ui/use-toast'
import { useFileUpload, type UploadedFile } from '@/hooks/useFileUpload'
import { useCreateProject } from '@/hooks/useProjects'
import { ArrowLeft, Loader2 } from 'lucide-react'

const projectSchema = z.object({
  projectName: z.string().min(1, 'Project name is required'),
  basePackageName: z.string().min(1, 'Base package name is required'),
  excludePackages: z.string(),
})

type ProjectFormData = z.infer<typeof projectSchema>

export function ProjectCreatePage() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const { extractZip } = useFileUpload()
  const createProject = useCreateProject()

  const [jarFile, setJarFile] = useState<UploadedFile | null>(null)
  const [sourceZipFile, setSourceZipFile] = useState<UploadedFile | null>(null)
  const [extractedDirId, setExtractedDirId] = useState<string | null>(null)

  const form = useForm<ProjectFormData>({
    resolver: zodResolver(projectSchema),
    defaultValues: {
      projectName: '',
      basePackageName: '',
      excludePackages: '',
    },
  })

  const handleJarFileSelected = (file: UploadedFile | null) => {
    setJarFile(file)
  }

  const handleSourceZipFileSelected = async (file: UploadedFile | null) => {
    setSourceZipFile(file)
    if (file) {
      // Automatically extract ZIP after selection
      const dirId = await extractZip(file.fileId)
      if (dirId) {
        setExtractedDirId(dirId)
      }
    } else {
      setExtractedDirId(null)
    }
  }

  const onSubmit = async (data: ProjectFormData) => {
    if (!jarFile) {
      toast({
        variant: 'destructive',
        title: 'JAR file required',
        description: 'Please upload a JAR file',
      })
      return
    }

    try {
      const result = await createProject.mutateAsync({
        data: {
          projectName: data.projectName,
          basePackageName: data.basePackageName,
          excludePackages: data.excludePackages
            .split('\n')
            .map((line) => line.trim())
            .filter(Boolean),
          jarFileId: jarFile.fileId,
          projectDirId: extractedDirId || undefined,
        },
      })

      toast({
        title: 'Project created',
        description: 'Your project has been created successfully',
      })

      navigate(`/projects/${result.projectId}`)
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to create project',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/projects">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold">Create Project</h1>
          <p className="text-muted-foreground mt-2">
            Analyze a Java monolithic application
          </p>
        </div>
      </div>

      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        {/* File Uploads */}
        <Card>
          <CardHeader>
            <CardTitle>Files</CardTitle>
            <CardDescription>Upload your JAR file and optionally your source code</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* JAR Upload */}
            <FileSelector
              id="jar-file"
              label="JAR File"
              accept=".jar"
              required
              onFileSelected={handleJarFileSelected}
              selectedFile={jarFile}
              mimeTypeFilter="jar"
            />

            {/* Source ZIP Upload */}
            <FileSelector
              id="source-zip"
              label="Source ZIP"
              accept=".zip"
              onFileSelected={handleSourceZipFileSelected}
              selectedFile={sourceZipFile}
              mimeTypeFilter="zip"
            />

            {extractedDirId && sourceZipFile && (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Loader2 className="h-4 w-4 text-green-500" />
                ZIP extracted successfully
              </div>
            )}
          </CardContent>
        </Card>

        {/* Project Configuration */}
        <Card>
          <CardHeader>
            <CardTitle>Configuration</CardTitle>
            <CardDescription>Configure your project settings</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="projectName">Project Name</Label>
              <Input
                id="projectName"
                {...form.register('projectName')}
                placeholder="My Application"
              />
              {form.formState.errors.projectName && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.projectName.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="basePackageName">Base Package Name</Label>
              <Input
                id="basePackageName"
                {...form.register('basePackageName')}
                placeholder="com.example.app"
              />
              {form.formState.errors.basePackageName && (
                <p className="text-sm text-destructive">
                  {form.formState.errors.basePackageName.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="excludePackages">Exclude Packages (one per line)</Label>
              <Textarea
                id="excludePackages"
                {...form.register('excludePackages')}
                placeholder="com.example.app.test&#10;com.example.app.generated"
                rows={4}
              />
              <p className="text-sm text-muted-foreground">
                Package patterns to exclude from analysis
              </p>
            </div>
          </CardContent>
        </Card>

        <div className="flex gap-4">
          <Button type="submit" disabled={createProject.isPending || !jarFile}>
            {createProject.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
            Create Project
          </Button>
          <Link to="/projects">
            <Button type="button" variant="outline">
              Cancel
            </Button>
          </Link>
        </div>
      </form>
    </div>
  )
}
