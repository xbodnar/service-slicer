import { Link, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { ArrowLeft, Loader2, PlayCircle, CheckCircle, XCircle, Clock } from 'lucide-react'
import { format } from 'date-fns'
import {useListBenchmarkRuns} from "@/api/generated/benchmark-run-controller/benchmark-run-controller.ts";
import {BenchmarkRunDto} from "@/api/generated/openAPIDefinition.schemas.ts";
import {useState} from "react";
import {Pagination} from "@/components/ui/pagination.tsx";

const getStateColor = (state: string) => {
  switch (state) {
    case 'COMPLETED':
      return 'default'
    case 'FAILED':
      return 'destructive'
    case 'PENDING':
      return 'secondary'
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
      return <CheckCircle className="h-4 w-4" />
    case 'FAILED':
      return <XCircle className="h-4 w-4" />
    case 'PENDING':
      return <Clock className="h-4 w-4" />
    default:
      return <PlayCircle className="h-4 w-4" />
  }
}

export function BenchmarkRunListPage() {
  const { benchmarkId } = useParams<{ benchmarkId: string }>()
    const [page, setPage] = useState(0)
    const [size] = useState(10)
  const { data, isLoading, error } = useListBenchmarkRuns({ benchmarkId: benchmarkId!, page: page, size: size})

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
          Error loading benchmark runs: {(error as Error)?.message || 'Unknown error'}
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link to={`/benchmarks/${benchmarkId}`}>
          <Button variant="outline" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-bold">Benchmark Runs</h1>
          <p className="text-muted-foreground mt-1">View all runs for this benchmark</p>
        </div>
      </div>

      {/* Runs List */}
      <Card>
        <CardHeader>
          <CardTitle>All Runs ({data.totalElements})</CardTitle>
        </CardHeader>
        <CardContent>
          {data.items.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-muted-foreground">No benchmark runs yet</p>
              <p className="text-sm text-muted-foreground mt-1">
                Run a benchmark to see results here
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {data.items.map((run: BenchmarkRunDto) => (
                <Link
                  key={run.id}
                  to={`/benchmarks/${benchmarkId}/runs/${run.id}`}
                  className="block"
                >
                  <div className="p-4 rounded-lg border bg-card hover:bg-accent transition-colors">
                    <div className="flex items-start justify-between">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-sm">{run.id}</span>
                          <Badge variant={getStateColor(run.status)} className={`flex items-center gap-1 ${getStateClassName(run.status)}`}>
                            {getStateIcon(run.status)}
                            {run.status}
                          </Badge>
                        </div>
                        <div className="flex items-center gap-4 text-sm text-muted-foreground">
                          <span>Created {format(new Date(run.createdAt), 'PPpp')}</span>
                        </div>
                      </div>
                      <Button variant="ghost" size="sm">
                        View Details
                      </Button>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

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
