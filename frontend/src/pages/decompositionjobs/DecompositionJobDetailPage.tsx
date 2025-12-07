import { useParams, Link } from 'react-router-dom'
import { useState, useMemo } from 'react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Loader2, ArrowLeft } from 'lucide-react'
import { useToast } from '@/components/ui/use-toast'
import { DependencyGraphVisualization } from '@/components/graph/DependencyGraphVisualization'
import {useDecompositionJob} from "@/hooks/useDecompositionJobs.ts";

type DecompositionMethod = 'labelPropagation' | 'louvain' | 'leiden' | 'domainDriven' | 'actorDriven'

export function DecompositionJobDetailPage() {
    const { decompositionJobId } = useParams<{ decompositionJobId: string }>()
    const { data, isLoading, error } = useDecompositionJob(decompositionJobId!)
    const { toast } = useToast()
    const [selectedMethod, setSelectedMethod] = useState<DecompositionMethod>('labelPropagation')

    // Create cluster mapping for the selected decomposition method
    // Must be called before early returns to satisfy Rules of Hooks
    const clusterMapping = useMemo(() => {
        if (!data?.decompositionResults) return {}

        const mapping: Record<string, string> = {}
        const results = data.decompositionResults[selectedMethod]

        if (results) {
            Object.entries(results).forEach(([clusterId, classNames]) => {
                classNames.forEach((className) => {
                    mapping[className] = clusterId
                })
            })
        }

        return mapping
    }, [data?.decompositionResults, selectedMethod])

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

    const { decompositionJob, dependencyGraph, decompositionResults } = data

    return (
        <div className="space-y-6">
            <div className="flex items-center gap-4">
                <Link to="/decomposition-jobs">
                    <Button variant="outline" size="icon" className="h-9 w-9 flex-shrink-0">
                        <ArrowLeft className="h-4 w-4" />
                    </Button>
                </Link>
                <div className="flex-1">
                    <h1 className="text-3xl font-bold">{decompositionJob.name}</h1>
                </div>
            </div>

            <Card>
                <CardHeader>
                    <CardTitle>Dependency Graph</CardTitle>
                    <CardDescription>Interactive visualization of class dependencies</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                    <div className="grid grid-cols-3 gap-4 items-end">
                        <div>
                            <p className="text-sm text-muted-foreground">Nodes</p>
                            <p className="text-2xl font-bold">{dependencyGraph.nodeCount}</p>
                        </div>
                        <div>
                            <p className="text-sm text-muted-foreground">Edges</p>
                            <p className="text-2xl font-bold">{dependencyGraph.edgeCount}</p>
                        </div>
                        <div>
                            <p className="text-sm text-muted-foreground mb-2">Color by Decomposition Method</p>
                            <Select value={selectedMethod} onValueChange={(value) => setSelectedMethod(value as DecompositionMethod)}>
                                <SelectTrigger className="w-full">
                                    <SelectValue placeholder="Select method" />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="labelPropagation">Label Propagation</SelectItem>
                                    <SelectItem value="louvain">Louvain</SelectItem>
                                    <SelectItem value="leiden">Leiden</SelectItem>
                                    <SelectItem value="domainDriven">Domain-Driven</SelectItem>
                                    <SelectItem value="actorDriven">Actor-Driven</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>
                    </div>

                    <DependencyGraphVisualization
                        nodes={dependencyGraph.nodes}
                        clusterMapping={clusterMapping}
                        clusters={decompositionResults[selectedMethod]}
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

            <Card>
                <CardHeader>
                    <CardTitle>Decomposition Results</CardTitle>
                    <CardDescription>Community detection algorithms</CardDescription>
                </CardHeader>
                <CardContent>
                    <Tabs value={selectedMethod} onValueChange={(value) => setSelectedMethod(value as DecompositionMethod)}>
                        <TabsList className="grid w-full grid-cols-5">
                            <TabsTrigger value="labelPropagation">Label Propagation</TabsTrigger>
                            <TabsTrigger value="louvain">Louvain</TabsTrigger>
                            <TabsTrigger value="leiden">Leiden</TabsTrigger>
                            <TabsTrigger value="domainDriven">Domain-Driven</TabsTrigger>
                            <TabsTrigger value="actorDriven">Actor-Driven</TabsTrigger>
                        </TabsList>

                        <TabsContent value="labelPropagation">
                            <DecompositionView results={decompositionResults.labelPropagation} />
                        </TabsContent>
                        <TabsContent value="louvain">
                            <DecompositionView results={decompositionResults.louvain} />
                        </TabsContent>
                        <TabsContent value="leiden">
                            <DecompositionView results={decompositionResults.leiden} />
                        </TabsContent>
                        <TabsContent value="domainDriven">
                            <DecompositionView results={decompositionResults.domainDriven} />
                        </TabsContent>
                        <TabsContent value="actorDriven">
                            <DecompositionView results={decompositionResults.actorDriven} />
                        </TabsContent>
                    </Tabs>
                </CardContent>
            </Card>
        </div>
    )
}

function DecompositionView({ results }: { results: Record<string, string[]> }) {
    const communities = Object.entries(results)

    if (communities.length === 0) {
        return (
            <div className="text-center py-8 text-muted-foreground">
                No communities detected
            </div>
        )
    }

    return (
        <div className="space-y-4 mt-4">
            {communities.map(([communityId, classes]) => (
                <Card key={communityId}>
                    <CardHeader>
                        <CardTitle className="text-lg">Cluster {communityId}</CardTitle>
                        <CardDescription>{classes.length} classes</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="max-h-40 overflow-y-auto">
                            <ul className="text-sm space-y-1">
                                {classes.map((className, idx) => (
                                    <li key={idx} className="text-muted-foreground font-mono text-xs">
                                        {className}
                                    </li>
                                ))}
                            </ul>
                        </div>
                    </CardContent>
                </Card>
            ))}
        </div>
    )
}
