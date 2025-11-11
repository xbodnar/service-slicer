import { useParams, Link } from 'react-router-dom'
import { useExperiment } from '@/hooks/useExperiments'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Loader2, ArrowLeft } from 'lucide-react'

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
          Error loading experiment: {error?.message || 'Unknown error'}
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/experiments">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div className="flex-1">
          <h1 className="text-3xl font-bold">{data.name}</h1>
          {data.description && (
            <p className="text-muted-foreground mt-2">{data.description}</p>
          )}
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Load Test Configuration</CardTitle>
          <CardDescription>{data.loadTestConfig.name}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <p className="text-sm font-medium">OpenAPI File ID</p>
            <p className="text-sm text-muted-foreground font-mono">
              {data.loadTestConfig.openApiFileId}
            </p>
          </div>
          {data.loadTestConfig.behaviorModels.length > 0 && (
            <div>
              <p className="text-sm font-medium mb-2">Behavior Models</p>
              <div className="space-y-2">
                {data.loadTestConfig.behaviorModels.map((model) => (
                  <Card key={model.id}>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm">
                        {model.id} - {model.actor}
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="text-sm space-y-1">
                      <p>Usage Profile: {(model.usageProfile * 100).toFixed(0)}%</p>
                      <p>Steps: {model.steps.join(' → ')}</p>
                      <p>Think Time: {model.thinkFrom}-{model.thinkTo}ms</p>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}
          {data.loadTestConfig.operationalProfile && (
            <div>
              <p className="text-sm font-medium mb-2">Operational Profile</p>
              <Card>
                <CardContent className="pt-6 text-sm">
                  <p>Loads: {data.loadTestConfig.operationalProfile.loads.join(', ')}</p>
                  <p>
                    Frequencies:{' '}
                    {data.loadTestConfig.operationalProfile.freq
                      .map((f) => (f * 100).toFixed(0) + '%')
                      .join(', ')}
                  </p>
                </CardContent>
              </Card>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Systems Under Test</CardTitle>
          <CardDescription>
            {data.systemsUnderTest.length} system{data.systemsUnderTest.length !== 1 ? 's' : ''}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {data.systemsUnderTest.map((system) => (
              <Card key={system.systemUnderTestId}>
                <CardHeader>
                  <CardTitle className="text-lg">{system.name}</CardTitle>
                  {system.description && (
                    <CardDescription>{system.description}</CardDescription>
                  )}
                </CardHeader>
                <CardContent className="space-y-2 text-sm">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="font-medium">Docker Compose</p>
                      <p className="text-muted-foreground font-mono">{system.composeFileId}</p>
                    </div>
                    <div>
                      <p className="font-medium">JAR File</p>
                      <p className="text-muted-foreground font-mono">{system.jarFileId}</p>
                    </div>
                    <div>
                      <p className="font-medium">Health Check Path</p>
                      <p className="text-muted-foreground">{system.healthCheckPath}</p>
                    </div>
                    <div>
                      <p className="font-medium">App Port</p>
                      <p className="text-muted-foreground">{system.appPort}</p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
