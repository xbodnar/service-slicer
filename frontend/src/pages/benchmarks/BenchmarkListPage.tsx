import { Link } from 'react-router-dom'
import { useBenchmarksList } from '@/hooks/useBenchmarks'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Plus, Loader2, FlaskConical, ArrowRight, Calendar } from 'lucide-react'
import { formatDistance } from 'date-fns'
import type { BenchmarkSummary } from '@/api/generated/openAPIDefinition.schemas'

export function BenchmarkListPage() {
  const { data, isLoading, error } = useBenchmarksList()

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
        <p className="text-destructive">Error loading benchmarks: {(error as Error).message}</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Load Test Benchmarks</h1>
          <p className="text-muted-foreground mt-2">
            Compare performance across different microservice decompositions
          </p>
        </div>
        <Link to="/benchmarks/new">
          <Button size="lg" className="gap-2">
            <Plus className="h-4 w-4" />
            New Benchmark
          </Button>
        </Link>
      </div>

      {data?.benchmarks.length === 0 ? (
        <Card className="border-dashed border-2">
          <CardContent className="flex flex-col items-center justify-center py-16">
            <div className="rounded-full bg-muted p-4 mb-4">
              <FlaskConical className="h-8 w-8 text-muted-foreground" />
            </div>
            <h3 className="text-lg font-semibold mb-2">No benchmarks yet</h3>
            <p className="text-muted-foreground mb-6 text-center max-w-sm">
              Create your first load test benchmark to start comparing system performance
            </p>
            <Link to="/benchmarks/new">
              <Button size="lg" className="gap-2">
                <Plus className="h-4 w-4" />
                Create your first benchmark
              </Button>
            </Link>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {data?.benchmarks.map((benchmark: BenchmarkSummary) => (
            <Link key={benchmark.benchmarkId} to={`/benchmarks/${benchmark.benchmarkId}`}>
              <Card className="group border-2 hover:border-primary/50 hover:shadow-lg transition-all h-full cursor-pointer">
                <CardHeader>
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div className="p-2 rounded-lg bg-primary/10 group-hover:bg-primary/20 transition-colors">
                      <FlaskConical className="h-5 w-5 text-primary" />
                    </div>
                    <ArrowRight className="h-5 w-5 text-muted-foreground group-hover:text-primary group-hover:translate-x-1 transition-all" />
                  </div>
                  <CardTitle className="group-hover:text-primary transition-colors line-clamp-2">
                    {benchmark.name}
                  </CardTitle>
                  {benchmark.description && (
                    <CardDescription className="line-clamp-2 mt-2">
                      {benchmark.description}
                    </CardDescription>
                  )}
                </CardHeader>
                <CardContent>
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Calendar className="h-3.5 w-3.5" />
                    <span>
                      {formatDistance(new Date(benchmark.createdAt), new Date(), { addSuffix: true })}
                    </span>
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
