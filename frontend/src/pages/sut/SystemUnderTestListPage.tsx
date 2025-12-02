import { Link } from 'react-router-dom'
import { useListSystemsUnderTest } from '@/api/generated/system-under-test-controller/system-under-test-controller'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Plus, Loader2, Server, ArrowRight, Calendar } from 'lucide-react'
import type { SystemUnderTestDto } from '@/api/generated/openAPIDefinition.schemas'

export function SystemUnderTestListPage() {
  const { data, isLoading, error } = useListSystemsUnderTest()

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
        <p className="text-destructive">Error loading systems under test: {(error as Error).message}</p>
      </div>
    )
  }

  // Type assertion to handle the Result type properly
  const systems = (data as any)?.systemsUnderTest as SystemUnderTestDto[] | undefined

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Systems Under Test</h1>
          <p className="text-muted-foreground mt-2">
            Manage and configure different system architectures for benchmarking
          </p>
        </div>
        <Link to="/systems-under-test/new">
          <Button size="lg" className="gap-2">
            <Plus className="h-4 w-4" />
            New System
          </Button>
        </Link>
      </div>

      {!systems || systems.length === 0 ? (
        <Card className="border-dashed border-2">
          <CardContent className="flex flex-col items-center justify-center py-16">
            <div className="rounded-full bg-muted p-4 mb-4">
              <Server className="h-8 w-8 text-muted-foreground" />
            </div>
            <h3 className="text-lg font-semibold mb-2">No systems yet</h3>
            <p className="text-muted-foreground mb-6 text-center max-w-sm">
              Create your first system under test to start benchmarking
            </p>
            <Link to="/systems-under-test/new">
              <Button size="lg" className="gap-2">
                <Plus className="h-4 w-4" />
                Create your first system
              </Button>
            </Link>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {systems.map((system) => (
            <Link key={system.id} to={`/systems-under-test/${system.id}`}>
              <Card className="group border-2 hover:border-primary/50 hover:shadow-lg transition-all h-full cursor-pointer">
                <CardHeader>
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div className="p-2 rounded-lg bg-primary/10 group-hover:bg-primary/20 transition-colors">
                      <Server className="h-5 w-5 text-primary" />
                    </div>
                    <ArrowRight className="h-5 w-5 text-muted-foreground group-hover:text-primary group-hover:translate-x-1 transition-all" />
                  </div>
                  <CardTitle className="group-hover:text-primary transition-colors line-clamp-2">
                    {system.name}
                  </CardTitle>
                  {system.description && (
                    <CardDescription className="line-clamp-2 mt-2">
                      {system.description}
                    </CardDescription>
                  )}
                </CardHeader>
                <CardContent>
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Calendar className="h-3.5 w-3.5" />
                    <span>Added recently</span>
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
