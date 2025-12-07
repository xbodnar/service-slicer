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
import { type UploadedFile } from '@/hooks/useFileUpload'
import { ArrowLeft, Loader2 } from 'lucide-react'
import {useCreateDecompositionJob} from "@/api/generated/decomposition-job-controller/decomposition-job-controller.ts";

const decompositionJobSchema = z.object({
    name: z.string().min(1, 'Decomposition Job name is required'),
    basePackageName: z.string().min(1, 'Base package name is required'),
    excludePackages: z.string(),
})

type DecompositionJobFormData = z.infer<typeof decompositionJobSchema>

export function DecompositionJobCreatePage() {
    const navigate = useNavigate()
    const { toast } = useToast()
    const createDecompositionJob = useCreateDecompositionJob()

    const [jarFile, setJarFile] = useState<UploadedFile | null>(null)

    const form = useForm<DecompositionJobFormData>({
        resolver: zodResolver(decompositionJobSchema),
        defaultValues: {
            name: '',
            basePackageName: '',
            excludePackages: '',
        },
    })

    const handleJarFileSelected = (file: UploadedFile | null) => {
        setJarFile(file)
    }

    const onSubmit = async (data: DecompositionJobFormData) => {
        if (!jarFile) {
            toast({
                variant: 'destructive',
                title: 'JAR file required',
                description: 'Please upload a JAR file',
            })
            return
        }

        try {
            const result = await createDecompositionJob.mutateAsync({
                data: {
                    name: data.name,
                    basePackageName: data.basePackageName,
                    excludePackages: data.excludePackages
                        .split('\n')
                        .map((line) => line.trim())
                        .filter(Boolean),
                    jarFileId: jarFile.fileId,
                },
            })

            toast({
                title: 'Decomposition Job created',
                description: 'Your decomposition job has been created successfully',
            })

            navigate(`/decomposition-jobs/${result.id}`)
        } catch (error) {
            toast({
                variant: 'destructive',
                title: 'Failed to create decomposition job',
                description: error instanceof Error ? error.message : 'An unknown error occurred',
            })
        }
    }

    return (
        <div className="max-w-3xl mx-auto space-y-6">
            <div className="flex items-center gap-4">
                <Link to="/decomposition-jobs">
                    <Button variant="ghost" size="icon">
                        <ArrowLeft className="h-4 w-4" />
                    </Button>
                </Link>
                <div>
                    <h1 className="text-3xl font-bold">Create Decomposition Job</h1>
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
                        <CardDescription>Upload your JAR file</CardDescription>
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
                    </CardContent>
                </Card>

                {/* Decomposition Job */}
                <Card>
                    <CardHeader>
                        <CardTitle>Configuration</CardTitle>
                        <CardDescription>Configure your decomposition job settings</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="name">Decomposition Job Name</Label>
                            <Input
                                id="name"
                                {...form.register('name')}
                                placeholder="My Application"
                            />
                            {form.formState.errors.name && (
                                <p className="text-sm text-destructive">
                                    {form.formState.errors.name.message}
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
                    <Button type="submit" disabled={createDecompositionJob.isPending || !jarFile}>
                        {createDecompositionJob.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                        Create Decomposition Job
                    </Button>
                    <Link to="/decomposition-jobs">
                        <Button type="button" variant="outline">
                            Cancel
                        </Button>
                    </Link>
                </div>
            </form>
        </div>
    )
}
