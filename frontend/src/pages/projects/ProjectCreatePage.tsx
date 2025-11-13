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
import { FileInput } from '@/components/ui/file-input'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/components/ui/use-toast'
import { useFileUpload, type UploadedFile } from '@/hooks/useFileUpload'
import { useCreateProject } from '@/hooks/useProjects'
import { ArrowLeft, Loader2, FileArchive, Package, CheckCircle2 } from 'lucide-react'

const projectSchema = z.object({
  projectName: z.string().min(1, 'Project name is required'),
  basePackageName: z.string().min(1, 'Base package name is required'),
  excludePackages: z.string(),
})

type ProjectFormData = z.infer<typeof projectSchema>

export function ProjectCreatePage() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const { uploadFile, extractZip, isUploading } = useFileUpload()
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

  const handleJarUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    const result = await uploadFile(file)
    if (result) {
      setJarFile(result)
    }
  }

  const handleSourceZipUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    const result = await uploadFile(file)
    if (result) {
      setSourceZipFile(result)
      // Automatically extract ZIP after upload
      const dirId = await extractZip(result.fileId)
      if (dirId) {
        setExtractedDirId(dirId)
      }
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
          <CardContent className="space-y-4">
            {/* JAR Upload */}
            <div className="space-y-2">
              <Label htmlFor="jar-file">JAR File (required)</Label>
              <FileInput
                id="jar-file"
                accept=".jar"
                onChange={handleJarUpload}
                disabled={isUploading}
              />
              {jarFile && (
                <div className="flex items-center gap-2 p-3 rounded-lg bg-muted/50 border">
                  <Package className="h-4 w-4 text-primary flex-shrink-0" />
                  <span className="text-sm font-medium flex-1 truncate">{jarFile.filename}</span>
                  <Badge variant="secondary" className="text-xs">
                    {(jarFile.size / 1024 / 1024).toFixed(2)} MB
                  </Badge>
                  <CheckCircle2 className="h-4 w-4 text-green-500 flex-shrink-0" />
                </div>
              )}
            </div>

            {/* Source ZIP Upload */}
            <div className="space-y-2">
              <Label htmlFor="source-zip">Source ZIP (optional)</Label>
              <FileInput
                id="source-zip"
                accept=".zip"
                onChange={handleSourceZipUpload}
                disabled={isUploading}
              />
              {sourceZipFile && (
                <div className="flex items-center gap-2 p-3 rounded-lg bg-muted/50 border">
                  <FileArchive className="h-4 w-4 text-primary flex-shrink-0" />
                  <span className="text-sm font-medium flex-1 truncate">{sourceZipFile.filename}</span>
                  <Badge variant="secondary" className="text-xs">
                    {(sourceZipFile.size / 1024 / 1024).toFixed(2)} MB
                  </Badge>
                  {extractedDirId && (
                    <Badge variant="outline" className="text-xs gap-1">
                      <CheckCircle2 className="h-3 w-3" />
                      Extracted
                    </Badge>
                  )}
                </div>
              )}
            </div>

            {isUploading && (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin" />
                Uploading...
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
