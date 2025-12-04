import { useParams, Link, useNavigate } from 'react-router-dom'
import {
  useGetSystemUnderTest,
  useDeleteSystemUnderTest,
} from '@/api/generated/system-under-test-controller/system-under-test-controller'
import type { DatabaseSeedConfigDto } from '@/api/generated/openAPIDefinition.schemas'
import { useToast } from '@/components/ui/use-toast'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Loader2,
  ArrowLeft,
  Server,
  FileArchive,
  FileCode,
  Trash2,
} from 'lucide-react'

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`
}

export function SystemUnderTestDetailPage() {
  const { sutId } = useParams<{ sutId: string }>()
  const navigate = useNavigate()
  const { data, isLoading, error } = useGetSystemUnderTest(sutId!)
  const { toast } = useToast()
  const deleteSystemUnderTest = useDeleteSystemUnderTest()

  const handleDelete = async () => {
    if (!data) return

    if (!confirm(`Are you sure you want to delete "${data.name}"?`)) {
      return
    }

    try {
      await deleteSystemUnderTest.mutateAsync({ sutId: sutId! })

      toast({
        title: 'System deleted',
        description: `"${data.name}" has been deleted successfully`,
      })

      navigate('/systems-under-test')
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to delete system',
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
          Error loading system: {(error as Error)?.message || 'Unknown error'}
        </p>
      </div>
    )
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
            <h1 className="text-2xl font-bold">{data.name}</h1>
            {data.description && <p className="text-muted-foreground mt-1">{data.description}</p>}
          </div>
        </div>
        <Button
          variant="destructive"
          onClick={handleDelete}
          disabled={deleteSystemUnderTest.isPending}
        >
          {deleteSystemUnderTest.isPending ? (
            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
          ) : (
            <Trash2 className="h-4 w-4 mr-2" />
          )}
          Delete
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-6">
        {/* Docker Configuration */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <Server className="h-5 w-5 text-primary" />
              <CardTitle>Docker Configuration</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Compose File */}
            {data.dockerConfig?.composeFile && (
              <div className="flex items-start gap-2 p-3 rounded-lg bg-muted/50">
                <FileArchive className="h-4 w-4 text-muted-foreground mt-0.5" />
                <div className="flex-1">
                  <p className="text-xs text-muted-foreground">Docker Compose File</p>
                  <span className="font-mono text-sm">{data.dockerConfig.composeFile.filename}</span>
                  <Badge variant="secondary" className="ml-2 text-xs">
                    {formatFileSize(data.dockerConfig.composeFile.fileSize)}
                  </Badge>
                </div>
              </div>
            )}

            {/* Config Details */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 p-3 rounded-lg bg-muted/50">
              <div>
                <p className="text-xs text-muted-foreground mb-1">Health Check Path</p>
                <p className="font-mono text-sm">{data.dockerConfig?.healthCheckPath}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground mb-1">App Port</p>
                <p className="text-sm">{data.dockerConfig?.appPort}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground mb-1">Startup Timeout</p>
                <p className="text-sm">{data.dockerConfig?.startupTimeoutSeconds}s</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Database Configurations */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <FileCode className="h-5 w-5 text-primary" />
              <CardTitle>Database Configurations</CardTitle>
              {data.databaseSeedConfigs && (
                <Badge variant="outline">{data.databaseSeedConfigs.length}</Badge>
              )}
            </div>
          </CardHeader>
          <CardContent>
            {data.databaseSeedConfigs && data.databaseSeedConfigs.length > 0 ? (
              <div className="space-y-4">
                {data.databaseSeedConfigs.map((dbConfig: DatabaseSeedConfigDto, index: number) => (
                  <div key={index} className="p-4 rounded-lg bg-muted/50 border space-y-3">
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" className="text-xs">
                        Database {index + 1}
                      </Badge>
                    </div>

                    {/* SQL Seed File */}
                    {dbConfig.sqlSeedFile && (
                      <div className="flex items-start gap-2 p-3 rounded-lg bg-background/50">
                        <FileCode className="h-4 w-4 text-muted-foreground mt-0.5" />
                        <div className="flex-1">
                          <p className="text-xs text-muted-foreground">SQL Seed File</p>
                          <span className="font-mono text-sm">{dbConfig.sqlSeedFile.filename}</span>
                          <Badge variant="secondary" className="ml-2 text-xs">
                            {formatFileSize(dbConfig.sqlSeedFile.fileSize)}
                          </Badge>
                        </div>
                      </div>
                    )}

                    {/* DB Config Details */}
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">Container Name</p>
                        <p className="font-mono text-sm">{dbConfig.dbContainerName}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">Port</p>
                        <p className="font-mono text-sm">{dbConfig.dbPort}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">Database Name</p>
                        <p className="font-mono text-sm">{dbConfig.dbName}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">Username</p>
                        <p className="font-mono text-sm">{dbConfig.dbUsername}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="flex items-center justify-center py-8">
                <p className="text-sm text-muted-foreground italic">No database configurations</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
