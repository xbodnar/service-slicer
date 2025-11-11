import { Link } from 'react-router-dom'
import { useExperimentsList } from '@/hooks/useExperiments'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Plus, Loader2 } from 'lucide-react'
import { formatDistance } from 'date-fns'

export function ExperimentListPage() {
  const { data, isLoading, error } = useExperimentsList()

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-destructive">Error loading experiments: {error.message}</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Load Test Experiments</h1>
          <p className="text-muted-foreground mt-2">
            Compare performance across different microservice decompositions
          </p>
        </div>
        <Link to="/experiments/new">
          <Button>
            <Plus className="h-4 w-4 mr-2" />
            New Experiment
          </Button>
        </Link>
      </div>

      {data?.experiments.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <p className="text-muted-foreground mb-4">No experiments yet</p>
            <Link to="/experiments/new">
              <Button>Create your first experiment</Button>
            </Link>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {data?.experiments.map((experiment) => (
            <Link key={experiment.experimentId} to={`/experiments/${experiment.experimentId}`}>
              <Card className="hover:shadow-lg transition-shadow h-full">
                <CardHeader>
                  <CardTitle>{experiment.name}</CardTitle>
                  {experiment.description && (
                    <CardDescription>{experiment.description}</CardDescription>
                  )}
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-muted-foreground">
                    Created {formatDistance(new Date(experiment.createdAt), new Date(), { addSuffix: true })}
                  </p>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
