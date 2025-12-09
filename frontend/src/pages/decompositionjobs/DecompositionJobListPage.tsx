import {Calendar, FolderKanban, Loader2, Package2, Plus, CheckCircle, XCircle, Clock, Activity} from "lucide-react";
import {Link} from "react-router-dom";
import {Button} from "@/components/ui/button.tsx";
import {Badge} from "@/components/ui/badge.tsx";
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card.tsx";
import {formatDistance} from "date-fns";
import {useDecompositionJobsList} from "@/hooks/useDecompositionJobs.ts";
import {DecompositionJobDto} from "@/api/generated/openAPIDefinition.schemas.ts";
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
      return <Activity className="h-3 w-3 animate-pulse" />
    default:
      return null
  }
}

export function DecompositionJobListPage() {
    const [page, setPage] = useState(0)
    const [size] = useState(12)
    const { data, isLoading, error } = useDecompositionJobsList({ page, size })

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
                <p className="text-destructive">Error loading decomposition jobs: {(error as Error).message}</p>
            </div>
        )
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Decomposition Jobs</h1>
                    <p className="text-muted-foreground mt-2">
                        Manage your Java application decomposition jobs
                    </p>
                </div>
                <Link to="/decomposition-jobs/new">
                    <Button size="lg" className="gap-2">
                        <Plus className="h-4 w-4" />
                        New Decomposition Job
                    </Button>
                </Link>
            </div>

            {data?.items.length === 0 ? (
                <Card className="border-dashed border-2">
                    <CardContent className="flex flex-col items-center justify-center py-16">
                        <div className="rounded-full bg-muted p-4 mb-4">
                            <FolderKanban className="h-8 w-8 text-muted-foreground" />
                        </div>
                        <h3 className="text-lg font-semibold mb-2">No decomposition jobs yet</h3>
                        <p className="text-muted-foreground mb-6 text-center max-w-sm">
                            Create your first decomposition job to analyze Java applications and discover microservice boundaries
                        </p>
                        <Link to="/decomposition-jobs/new">
                            <Button size="lg" className="gap-2">
                                <Plus className="h-4 w-4" />
                                Create your first decomposition job
                            </Button>
                        </Link>
                    </CardContent>
                </Card>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                    {data?.items.map((decompositionJob: DecompositionJobDto) => (
                        <Link key={decompositionJob.id} to={`/decomposition-jobs/${decompositionJob.id}`}>
                            <Card className="group border-2 hover:border-primary/50 hover:shadow-lg transition-all h-full cursor-pointer">
                                <CardHeader>
                                    <div className="flex items-start justify-between gap-2 mb-2">
                                        <div className="p-2 rounded-lg bg-primary/10 group-hover:bg-primary/20 transition-colors">
                                            <FolderKanban className="h-5 w-5 text-primary" />
                                        </div>
                                        <Badge variant={getStateColor(decompositionJob.status)} className={`flex items-center gap-1 ${getStateClassName(decompositionJob.status)}`}>
                                            {getStateIcon(decompositionJob.status)}
                                            {decompositionJob.status}
                                        </Badge>
                                    </div>
                                    <CardTitle className="group-hover:text-primary transition-colors line-clamp-1">
                                        {decompositionJob.name}
                                    </CardTitle>
                                    <CardDescription className="flex items-center gap-1.5 mt-2">
                                        <Package2 className="h-3.5 w-3.5" />
                                        <span className="line-clamp-1">{decompositionJob.monolithArtifact.basePackageName}</span>
                                    </CardDescription>
                                </CardHeader>
                                <CardContent>
                                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                        <Calendar className="h-3.5 w-3.5" />
                                        <span>{formatDistance(new Date(decompositionJob.createdAt), new Date(), { addSuffix: true })}</span>
                                    </div>
                                </CardContent>
                            </Card>
                        </Link>
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
