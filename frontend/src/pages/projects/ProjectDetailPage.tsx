import { useParams, Link } from 'react-router-dom'
import { useProject, useRebuildProjectGraph } from '@/hooks/useProjects'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Loader2, ArrowLeft, RefreshCw } from 'lucide-react'
import { useToast } from '@/components/ui/use-toast'
import { DependencyGraphVisualization } from '@/components/graph/DependencyGraphVisualization'

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>()
  const { data, isLoading, error } = useProject(projectId!)
  const rebuildGraph = useRebuildProjectGraph()
  const { toast } = useToast()

  const handleRebuildGraph = async () => {
    try {
      await rebuildGraph.mutateAsync({ projectId: projectId! })
      toast({
        title: 'Graph rebuild started',
        description: 'The dependency graph is being rebuilt.',
      })
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Rebuild failed',
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
        <p className="text-destructive">Error loading project: {(error as Error)?.message || 'Unknown error'}</p>
      </div>
    )
  }

  const { staticAnalysis } = data.analysisJobResult

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/projects">
          <Button variant="outline" size="icon" className="h-9 w-9 flex-shrink-0">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div className="flex-1">
          <h1 className="text-3xl font-bold">{data.name}</h1>
        </div>
        <Button onClick={handleRebuildGraph} disabled={rebuildGraph.isPending}>
          <RefreshCw className={`h-4 w-4 mr-2 ${rebuildGraph.isPending ? 'animate-spin' : ''}`} />
          Rebuild Graph
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Dependency Graph</CardTitle>
          <CardDescription>Interactive visualization of class dependencies</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-sm text-muted-foreground">Nodes</p>
              <p className="text-2xl font-bold">{staticAnalysis.dependencyGraph.nodeCount}</p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Edges</p>
              <p className="text-2xl font-bold">{staticAnalysis.dependencyGraph.edgeCount}</p>
            </div>
          </div>

          <DependencyGraphVisualization
            nodes={staticAnalysis.dependencyGraph.nodes}
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
                          {node.dependencies.map((dep, idx) => (
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
          <Tabs defaultValue="labelPropagation">
            <TabsList className="grid w-full grid-cols-5">
              <TabsTrigger value="labelPropagation">Label Propagation</TabsTrigger>
              <TabsTrigger value="louvain">Louvain</TabsTrigger>
              <TabsTrigger value="leiden">Leiden</TabsTrigger>
              <TabsTrigger value="domain">Domain-Driven</TabsTrigger>
              <TabsTrigger value="actor">Actor-Driven</TabsTrigger>
            </TabsList>

            <TabsContent value="labelPropagation">
              <DecompositionView results={staticAnalysis.labelPropagationAlgorithm} />
            </TabsContent>
            <TabsContent value="louvain">
              <DecompositionView results={staticAnalysis.louvainAlgorithm} />
            </TabsContent>
            <TabsContent value="leiden">
              <DecompositionView results={staticAnalysis.leidenAlgorithm} />
            </TabsContent>
            <TabsContent value="domain">
              <DecompositionView results={staticAnalysis.domainDrivenDecomposition} />
            </TabsContent>
            <TabsContent value="actor">
              <DecompositionView results={staticAnalysis.actorDrivenDecomposition} />
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </div>
  )
}

interface DecompositionViewProps {
  results: { communities: Record<string, string[]> }
}

function DecompositionView({ results }: DecompositionViewProps) {
  const communities = Object.entries(results.communities)

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
            <CardTitle className="text-lg">Community {communityId}</CardTitle>
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
