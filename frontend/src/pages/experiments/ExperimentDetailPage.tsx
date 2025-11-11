import { useParams, Link } from 'react-router-dom'
import { useExperiment } from '@/hooks/useExperiments'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import {
  Loader2,
  ArrowLeft,
  FileCode,
  Server,
  Package,
  Activity,
  FileArchive,
  Users
} from 'lucide-react'

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`
}

export function ExperimentDetailPage() {
  const { experimentId } = useParams<{ experimentId: string }>()
  const { data, isLoading, error } = useExperiment(experimentId!)

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
            <div className="flex items-center gap-2">
              <div className="p-2 rounded-lg bg-primary/10">
                <Activity className="h-5 w-5 text-primary" />
              </div>
              <CardTitle>Load Test Configuration</CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-5">
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
                        {model.steps.join(' → ')} • Think: {model.thinkFrom}-{model.thinkTo}ms
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Operational Profile */}
            {data.loadTestConfig.operationalProfile && (
              <div className="space-y-2">
                <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                  <Activity className="h-4 w-4" />
                  <span>Operational Profile</span>
                </div>
                <div className="pl-6 text-sm space-y-1">
                  <p><span className="text-muted-foreground">Loads:</span> {data.loadTestConfig.operationalProfile.loads.join(', ')}</p>
                  <p>
                    <span className="text-muted-foreground">Frequencies:</span>{' '}
                    {data.loadTestConfig.operationalProfile.freq
                      .map((f) => (f * 100).toFixed(0) + '%')
                      .join(', ')}
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Systems Under Test */}
        <Card className="border-2 transition-shadow hover:shadow-md">
          <CardHeader>
            <div className="flex items-center gap-2">
              <div className="p-2 rounded-lg bg-primary/10">
                <Server className="h-5 w-5 text-primary" />
              </div>
              <CardTitle className="flex items-center gap-2">
                Systems Under Test
                <Badge variant="outline">{data.systemsUnderTest.length}</Badge>
              </CardTitle>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {data.systemsUnderTest.map((system, idx) => (
              <div
                key={system.systemUnderTestId}
                className={cn(
                  'space-y-3 p-4 rounded-lg bg-muted/50 transition-colors hover:bg-muted',
                  idx > 0 && 'mt-4'
                )}
              >
                <div>
                  <h4 className="font-semibold">{system.name}</h4>
                  {system.description && (
                    <p className="text-sm text-muted-foreground mt-0.5">
                      {system.description}
                    </p>
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
