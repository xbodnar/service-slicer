import { Link } from 'react-router-dom'
import { useListSystemsUnderTest } from '@/api/generated/system-under-test-controller/system-under-test-controller'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Plus, Loader2, Server, ArrowRight, Calendar, Container, Database, Clock, Activity } from 'lucide-react'
import {Pagination} from "@/components/ui/pagination.tsx";
import {useState} from "react";
import {formatDistance} from "date-fns";

export function SystemUnderTestListPage() {
  const [page, setPage] = useState(0)
  const [size] = useState(6)
  const { data, isLoading, error } = useListSystemsUnderTest({ page, size })

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

      {!data?.items || data.items.length === 0 ? (
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
          {data.items.map((system) => (
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
                <CardContent className="space-y-3">
                  {/* Docker Config */}
                  {system.dockerConfig && (
                    <div className="space-y-2">
                      <div className="flex items-center gap-2">
                        <Container className="h-4 w-4 text-muted-foreground" />
                        <span className="text-sm font-medium">Docker Configuration</span>
                      </div>
                      <div className="space-y-1 pl-6 text-xs text-muted-foreground">
                        <div className="flex items-center gap-2">
                          <Activity className="h-3 w-3" />
                          <span>Health check: {system.dockerConfig.healthCheckPath}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Server className="h-3 w-3" />
                          <span>App port: {system.dockerConfig.appPort}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Clock className="h-3 w-3" />
                          <span>Startup timeout: {system.dockerConfig.startupTimeoutSeconds}s</span>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Database Seed Configs */}
                  {system.databaseSeedConfigs && system.databaseSeedConfigs.length > 0 && (
                    <div className="space-y-2 pt-2 border-t">
                      <div className="flex items-center gap-2">
                        <Database className="h-4 w-4 text-muted-foreground" />
                        <span className="text-sm font-medium">Database Seeds ({system.databaseSeedConfigs.length})</span>
                      </div>
                      <div className="flex flex-wrap gap-1.5 pl-6">
                        {system.databaseSeedConfigs.map((config, idx) => (
                          <Badge key={idx} variant="secondary" className="text-xs">
                            {config.dbContainerName}:{config.dbPort}/{config.dbName}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="flex items-center gap-2 text-sm text-muted-foreground pt-2 border-t">
                    <Calendar className="h-3.5 w-3.5" />
                    <span>{formatDistance(new Date(system.createdAt), new Date(), { addSuffix: true })}</span>
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
