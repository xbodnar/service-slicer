import { Link } from 'react-router-dom'
import { useBenchmarksList } from '@/hooks/useBenchmarks'
import { useListBenchmarkRuns } from '@/api/generated/benchmark-run-controller/benchmark-run-controller'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Plus, Loader2, FlaskConical, ArrowRight, Calendar, CheckCircle, XCircle, Clock, PlayCircle, History } from 'lucide-react'
import { formatDistance } from 'date-fns'
import {BenchmarkDto} from "@/api/generated/openAPIDefinition.schemas.ts";
import {Pagination} from "@/components/ui/pagination.tsx";
import {useState} from "react";

const getStateColor = (state: string) => {
  switch (state) {
    case 'COMPLETED':
      return 'default'
    case 'FAILED':
      return 'destructive'
    case 'PENDING':
      return 'secondary'
    case 'RUNNING':
      return 'default'
    default:
      return 'outline'
  }
}

const getStateClassName = (state: string) => {
  return state === 'COMPLETED' ? 'bg-green-600 hover:bg-green-700' : ''
}

const getStateIcon = (state: string) => {
  switch (state) {
    case 'COMPLETED':
      return <CheckCircle className="h-3 w-3" />
    case 'FAILED':
      return <XCircle className="h-3 w-3" />
    case 'PENDING':
      return <Clock className="h-3 w-3" />
    case 'RUNNING':
      return <PlayCircle className="h-3 w-3" />
    default:
      return <History className="h-3 w-3" />
  }
}

// Component to fetch and display run info for a single benchmark
function BenchmarkRunInfo({ benchmarkId }: { benchmarkId: string }) {
  const { data: runsData } = useListBenchmarkRuns(
    { benchmarkId, page: 0, size: 1 },
    {
      query: {
        // Don't show loading state, just use stale data if available
        refetchOnWindowFocus: false,
      }
    }
  )

  const latestRun = runsData?.items?.[0]
  const totalRuns = runsData?.totalElements || 0

  if (totalRuns === 0) {
    return (
      <div className="text-xs text-muted-foreground italic">
        No runs yet
      </div>
    )
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2">
        <History className="h-3.5 w-3.5 text-muted-foreground" />
        <span className="text-xs text-muted-foreground">
          {totalRuns} {totalRuns === 1 ? 'run' : 'runs'}
        </span>
      </div>
      {latestRun && (
        <div className="flex items-center gap-2">
          <Badge variant={getStateColor(latestRun.status)} className={`flex items-center gap-1 text-xs ${getStateClassName(latestRun.status)}`}>
            {getStateIcon(latestRun.status)}
            {latestRun.status}
          </Badge>
          <span className="text-xs text-muted-foreground">
            {formatDistance(new Date(latestRun.createdAt), new Date(), { addSuffix: true })}
          </span>
        </div>
      )}
    </div>
  )
}

// Component to provide action buttons for benchmark runs
function BenchmarkRunActions({ benchmarkId }: { benchmarkId: string }) {
  const { data: runsData } = useListBenchmarkRuns(
    { benchmarkId, page: 0, size: 1 },
    {
      query: {
        refetchOnWindowFocus: false,
      }
    }
  )

  const latestRun = runsData?.items?.[0]
  const hasRuns = (runsData?.totalElements || 0) > 0

  if (!hasRuns || !latestRun) {
    return (
      <Link to={`/benchmarks/${benchmarkId}/runs`} onClick={(e) => e.stopPropagation()}>
        <Button variant="outline" size="sm" className="w-full">
          <History className="h-4 w-4 mr-2" />
          View Runs
        </Button>
      </Link>
    )
  }

  return (
    <div className="grid grid-cols-2 gap-2">
      <Link to={`/benchmarks/${benchmarkId}/runs/${latestRun.id}`} onClick={(e) => e.stopPropagation()}>
        <Button variant="default" size="sm" className="w-full">
          <PlayCircle className="h-4 w-4 mr-2" />
          Latest Run
        </Button>
      </Link>
      <Link to={`/benchmarks/${benchmarkId}/runs`} onClick={(e) => e.stopPropagation()}>
        <Button variant="outline" size="sm" className="w-full">
          <History className="h-4 w-4 mr-2" />
          All Runs
        </Button>
      </Link>
    </div>
  )
}

export function BenchmarkListPage() {
  const [page, setPage] = useState(0)
  const [size] = useState(12)
  const { data, isLoading, error } = useBenchmarksList({ page, size })

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

      {data?.items.length === 0 ? (
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
          {data?.items.map((benchmark: BenchmarkDto) => (
            <Card key={benchmark.id} className="group border-2 hover:border-primary/50 hover:shadow-lg transition-all h-full flex flex-col">
              <Link to={`/benchmarks/${benchmark.id}`} className="flex-1">
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
                <CardContent className="space-y-3">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Calendar className="h-3.5 w-3.5" />
                    <span>
                      {formatDistance(new Date(benchmark.createdAt), new Date(), { addSuffix: true })}
                    </span>
                  </div>
                  <div className="pt-2 border-t">
                    <BenchmarkRunInfo benchmarkId={benchmark.id} />
                  </div>
                </CardContent>
              </Link>
              <CardContent className="pt-0 mt-auto">
                <BenchmarkRunActions benchmarkId={benchmark.id} />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <Pagination
          currentPage={data.currentPage}
          totalPages={data.totalPages}
          pageSize={data.pageSize}
          totalElements={data.totalElements}
          onPageChange={setPage}
        />
      )}
    </div>
  )
}
