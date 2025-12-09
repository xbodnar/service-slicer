import { useParams, Link } from 'react-router-dom'
import { useState, useMemo } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Loader2, ArrowLeft, FileArchive, CheckCircle, XCircle, Clock, Activity, RotateCcw } from 'lucide-react'
import { useToast } from '@/components/ui/use-toast'
import { DependencyGraphVisualization } from '@/components/graph/DependencyGraphVisualization'
import {useDecompositionJob} from "@/hooks/useDecompositionJobs.ts";
import { useRestartDecompositionJob } from '@/api/generated/decomposition-job-controller/decomposition-job-controller'
import { useQueryClient } from '@tanstack/react-query'
import { formatDistanceToNow } from 'date-fns'

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
      return <CheckCircle className="h-4 w-4" />
    case 'FAILED':
      return <XCircle className="h-4 w-4" />
    case 'PENDING':
      return <Clock className="h-4 w-4" />
    case 'RUNNING':
      return <Activity className="h-4 w-4 animate-pulse" />
    default:
      return null
  }
}

export function DecompositionJobDetailPage() {
    const { decompositionJobId } = useParams<{ decompositionJobId: string }>()
    const { toast } = useToast()
    const queryClient = useQueryClient()
    const [selectedCandidateId, setSelectedCandidateId] = useState<string>('')

    // Fetch decomposition job with auto-refetch every 5 seconds when status is PENDING or RUNNING
    const { data, isLoading, error } = useDecompositionJob(decompositionJobId!, {
        refetchInterval: (query) => {
            // Poll while loading or if status is PENDING/RUNNING
            // The callback receives the Query object, not the data directly
            const actualData = query?.state?.data
            if (!actualData) {
                console.log('DecompositionJob: No data yet, polling in 5s')
                return 5000
            }
            const status = actualData.decompositionJob?.status
            const shouldPoll = status === 'PENDING' || status === 'RUNNING'
            console.log(`DecompositionJob: status=${status}, shouldPoll=${shouldPoll}`)
            return shouldPoll ? 5000 : false
        }
    })

    const restartJobMutation = useRestartDecompositionJob({
        mutation: {
            onSuccess: (updatedJob) => {
                toast({
                    title: 'Job restarted',
                    description: 'The decomposition job has been restarted successfully',
                })

                // Update the cache with the response from the restart API
                queryClient.setQueryData(
                    [`/decomposition-jobs/${decompositionJobId}`],
                    (oldData: any) => {
                        if (!oldData) return oldData
                        return {
                            ...oldData,
                            decompositionJob: updatedJob
                        }
                    }
                )

                // Invalidate list query to refresh the list page
                queryClient.invalidateQueries({ queryKey: ['/decomposition-jobs'], exact: true })
            },
            onError: (error: Error) => {
                toast({
                    variant: 'destructive',
                    title: 'Failed to restart job',
                    description: error.message || 'An unknown error occurred',
                })
            },
        },
    })

    const handleRestartJob = () => {
        if (!decompositionJobId) return
        restartJobMutation.mutate({ decompositionJobId })
    }

    // Set initial selected candidate when data loads
    useMemo(() => {
        if (data?.decompositionCandidates && data.decompositionCandidates.length > 0 && !selectedCandidateId) {
            setSelectedCandidateId(data.decompositionCandidates[0].id)
        }
    }, [data?.decompositionCandidates, selectedCandidateId])

    // Get the selected candidate
    const selectedCandidate = useMemo(() => {
        if (!data?.decompositionCandidates) return null
        return data.decompositionCandidates.find(c => c.id === selectedCandidateId) || null
    }, [data?.decompositionCandidates, selectedCandidateId])

    // Create cluster mapping for the selected candidate
    const clusterMapping = useMemo(() => {
        const mapping: Record<string, string> = {}

        if (selectedCandidate) {
            selectedCandidate.serviceBoundaries.forEach((boundary) => {
                boundary.typeNames.forEach((className) => {
                    mapping[className] = boundary.name
                })
            })
        }

        return mapping
    }, [selectedCandidate])

    // Create clusters object for graph visualization
    const clusters = useMemo(() => {
        if (!selectedCandidate) return {}

        const clustersObj: Record<string, string[]> = {}
        selectedCandidate.serviceBoundaries.forEach((boundary) => {
            clustersObj[boundary.name] = boundary.typeNames
        })

        return clustersObj
    }, [selectedCandidate])

    // Calculate graph statistics
    const graphStats = useMemo(() => {
        if (!data?.dependencyGraph) return { nodeCount: 0, edgeCount: 0 }

        const nodeCount = data.dependencyGraph.length
        const edgeCount = data.dependencyGraph.reduce((sum, node) => sum + node.dependencies.length, 0)

        return { nodeCount, edgeCount }
    }, [data?.dependencyGraph])

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
                <p className="text-destructive">Error loading decomposition job: {(error as Error)?.message || 'Unknown error'}</p>
            </div>
        )
    }

    const formatFileSize = (bytes: number) => {
        if (bytes < 1024) return `${bytes} B`
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`
        if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
        return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
    }

    const formatDateTime = (dateString?: string) => {
        if (!dateString) return 'N/A'
        return new Date(dateString).toLocaleString()
    }

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                    <Link to="/decomposition-jobs">
                        <Button variant="outline" size="icon" className="h-9 w-9 flex-shrink-0">
                            <ArrowLeft className="h-4 w-4" />
                        </Button>
                    </Link>
                    <div className="flex-1">
                        <h1 className="text-3xl font-bold">{data.decompositionJob.name}</h1>
                    </div>
                </div>
                <Button
                    variant="outline"
                    onClick={handleRestartJob}
                    disabled={data.decompositionJob.status !== 'FAILED' || restartJobMutation.isPending}
                >
                    {restartJobMutation.isPending ? (
                        <>
                            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                            Restarting...
                        </>
                    ) : (
                        <>
                            <RotateCcw className="h-4 w-4 mr-2" />
                            Restart Job
                        </>
                    )}
                </Button>
            </div>

            <Card>
                <CardHeader>
                    <CardTitle>Overview</CardTitle>
                    <CardDescription>Decomposition job details and configuration</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                <FileArchive className="h-4 w-4" />
                                JAR File
                            </div>
                            <p className="font-medium">{data.decompositionJob.monolithArtifact.jarFile.filename}</p>
                            <p className="text-sm text-muted-foreground">{formatFileSize(data.decompositionJob.monolithArtifact.jarFile.fileSize)}</p>
                        </div>
                        <div className="space-y-2">
                            <p className="text-sm text-muted-foreground">Status</p>
                            <Badge variant={getStateColor(data.decompositionJob.status)} className={`flex items-center gap-1 w-fit ${getStateClassName(data.decompositionJob.status)}`}>
                                {getStateIcon(data.decompositionJob.status)}
                                {data.decompositionJob.status}
                            </Badge>
                        </div>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <p className="text-sm text-muted-foreground">Base Package</p>
                            <p className="font-medium font-mono text-sm">{data.decompositionJob.monolithArtifact.basePackageName}</p>
                        </div>
                        <div className="space-y-2">
                            <p className="text-sm text-muted-foreground">Exclude Packages</p>
                            <p className="font-medium font-mono text-sm">
                                {data.decompositionJob.monolithArtifact.excludePackages.length > 0
                                    ? data.decompositionJob.monolithArtifact.excludePackages.join(', ')
                                    : 'None'}
                            </p>
                        </div>
                    </div>
                    {(data.decompositionJob.startTimestamp || data.decompositionJob.endTimestamp) && (
                        <div className="grid grid-cols-2 gap-4">
                            {data.decompositionJob.startTimestamp && (
                                <div className="space-y-2">
                                    <p className="text-sm text-muted-foreground">Started</p>
                                    <p className="font-medium">{formatDateTime(data.decompositionJob.startTimestamp)}</p>
                                </div>
                            )}
                            {data.decompositionJob.endTimestamp && (
                                <div className="space-y-2">
                                    <p className="text-sm text-muted-foreground">Ended</p>
                                    <p className="font-medium">{formatDateTime(data.decompositionJob.endTimestamp)}</p>
                                </div>
                            )}
                        </div>
                    )}
                    <div className="grid grid-cols-2 gap-4 pt-4 border-t">
                        <div className="space-y-2">
                            <p className="text-sm text-muted-foreground">Created</p>
                            <p className="font-medium">
                                {formatDistanceToNow(new Date(data.decompositionJob.createdAt), { addSuffix: true })}
                            </p>
                        </div>
                        <div className="space-y-2">
                            <p className="text-sm text-muted-foreground">Last Updated</p>
                            <p className="font-medium">
                                {formatDistanceToNow(new Date(data.decompositionJob.updatedAt), { addSuffix: true })}
                            </p>
                        </div>
                    </div>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle>Dependency Graph</CardTitle>
                    <CardDescription>Interactive visualization of class dependencies</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <p className="text-sm text-muted-foreground">Nodes</p>
                            <p className="text-2xl font-bold">{graphStats.nodeCount}</p>
                        </div>
                        <div>
                            <p className="text-sm text-muted-foreground">Edges</p>
                            <p className="text-2xl font-bold">{graphStats.edgeCount}</p>
                        </div>
                    </div>

                    {data.decompositionCandidates && data.decompositionCandidates.length > 0 && (
                        <div className="space-y-3">
                            <div>
                                <p className="text-sm text-muted-foreground mb-2">Decomposition Method</p>
                                <Tabs value={selectedCandidateId} onValueChange={setSelectedCandidateId}>
                                    <TabsList className="w-full">
                                        {data.decompositionCandidates.map((candidate) => (
                                            <TabsTrigger key={candidate.id} value={candidate.id} className="flex-1">
                                                {candidate.method.replace(/_/g, ' ').replace(/COMMUNITY DETECTION /, '').replace(/DECOMPOSITION/, '').trim()}
                                            </TabsTrigger>
                                        ))}
                                    </TabsList>
                                </Tabs>
                            </div>
                            <div className="p-3 rounded-lg bg-muted/50">
                                <div className="flex items-center justify-between">
                                    <div>
                                        <p className="text-sm font-semibold">Modularity</p>
                                        <p className="text-xs text-muted-foreground">Quality measure of the decomposition</p>
                                    </div>
                                    <p className="text-2xl font-bold">
                                        {selectedCandidate?.modularity !== undefined && selectedCandidate?.modularity !== null
                                            ? selectedCandidate.modularity.toFixed(4)
                                            : 'N/A'}
                                    </p>
                                </div>
                            </div>
                        </div>
                    )}

                    <DependencyGraphVisualization
                        nodes={data.dependencyGraph}
                        clusterMapping={clusterMapping}
                        clusters={clusters}
                        onNodeClick={(node) => {
                            toast({
                                title: node.simpleClassName,
                                description: (
                                    <div className="space-y-2">
                                        <p className="text-sm font-mono">{node.fullyQualifiedClassName}</p>
                                        {node.dependencies.length > 0 && (
                                            <div>
                                                <p className="text-sm font-semibold mb-1">Dependencies ({node.dependencies.length}):</p>
                                                <ul className="text-xs space-y-1 max-h-40 overflow-y-auto">
                                                    {node.dependencies.map((dep: string, idx: number) => (
                                                        <li key={idx} className="font-mono">{dep}</li>
                                                    ))}
                                                </ul>
                                            </div>
                                        )}
                                    </div>
                                ),
                            })
                        }}
                    />
                </CardContent>
            </Card>

            {selectedCandidate && (
                <Card>
                    <CardHeader>
                        <CardTitle>Service Boundaries</CardTitle>
                        <CardDescription>
                            Suggested microservice decomposition with {selectedCandidate.serviceBoundaries.length} service{selectedCandidate.serviceBoundaries.length !== 1 ? 's' : ''}
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-4">
                            {selectedCandidate.serviceBoundaries.map((boundary) => (
                                <Card key={boundary.id}>
                                    <CardHeader>
                                        <div className="flex items-start justify-between">
                                            <div>
                                                <CardTitle className="text-lg">{boundary.name}</CardTitle>
                                                <CardDescription>{boundary.typeNames.length} class{boundary.typeNames.length !== 1 ? 'es' : ''}</CardDescription>
                                            </div>
                                        </div>
                                    </CardHeader>
                                    <CardContent className="space-y-4">
                                        {/* Metrics */}
                                        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 p-3 rounded-lg bg-muted/50">
                                            <div className="text-center">
                                                <p className="text-xs text-muted-foreground">Size</p>
                                                <p className="text-lg font-bold">{boundary.metrics.size}</p>
                                            </div>
                                            <div className="text-center">
                                                <p className="text-xs text-muted-foreground">Cohesion</p>
                                                <p className="text-lg font-bold">{boundary.metrics.cohesion.toFixed(3)}</p>
                                            </div>
                                            <div className="text-center">
                                                <p className="text-xs text-muted-foreground">Coupling</p>
                                                <p className="text-lg font-bold">{boundary.metrics.coupling.toFixed(3)}</p>
                                            </div>
                                            <div className="text-center">
                                                <p className="text-xs text-muted-foreground">Internal Deps</p>
                                                <p className="text-lg font-bold">{boundary.metrics.internalDependencies}</p>
                                            </div>
                                            <div className="text-center">
                                                <p className="text-xs text-muted-foreground">External Deps</p>
                                                <p className="text-lg font-bold">{boundary.metrics.externalDependencies}</p>
                                            </div>
                                        </div>

                                        {/* Classes */}
                                        <div>
                                            <p className="text-sm font-semibold mb-2">Classes</p>
                                            <div className="max-h-48 overflow-y-auto rounded-lg border p-3 bg-background">
                                                <ul className="space-y-1">
                                                    {boundary.typeNames.map((className, idx) => (
                                                        <li key={idx} className="text-xs font-mono text-muted-foreground">
                                                            {className}
                                                        </li>
                                                    ))}
                                                </ul>
                                            </div>
                                        </div>
                                    </CardContent>
                                </Card>
                            ))}
                        </div>
                    </CardContent>
                </Card>
            )}
        </div>
    )
}
