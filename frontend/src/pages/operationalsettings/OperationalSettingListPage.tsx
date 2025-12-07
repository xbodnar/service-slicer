import { Link } from 'react-router-dom'
import { useListOperationalSettings } from '@/api/generated/operational-setting-controller/operational-setting-controller'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Loader2, Plus, Settings, ArrowRight, Calendar } from 'lucide-react'
import { formatDistance } from 'date-fns'
import { Pagination } from '@/components/ui/pagination'
import { useState } from 'react'

export function OperationalSettingListPage() {
  const [page, setPage] = useState(0)
  const [size] = useState(12)
  const { data, isLoading, error } = useListOperationalSettings({ page, size })

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
        <p className="text-destructive">Error loading operational settings: {(error as Error).message}</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Operational Settings</h1>
          <p className="text-muted-foreground mt-2">
            Reusable load test configurations
          </p>
        </div>
        <Link to="/operational-settings/new">
          <Button size="lg" className="gap-2">
            <Plus className="h-4 w-4" />
            New Config
          </Button>
        </Link>
      </div>

      {!data?.items || data.items.length === 0 ? (
        <Card className="border-dashed border-2">
          <CardContent className="flex flex-col items-center justify-center py-16">
            <div className="rounded-full bg-muted p-4 mb-4">
              <Settings className="h-8 w-8 text-muted-foreground" />
            </div>
            <h3 className="text-lg font-semibold mb-2">No operational settings yet</h3>
            <p className="text-muted-foreground mb-6 text-center max-w-sm">
              Create reusable load test configurations
            </p>
            <Link to="/operational-settings/new">
              <Button size="lg" className="gap-2">
                <Plus className="h-4 w-4" />
                Create your first config
              </Button>
            </Link>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {data.items.map((config) => (
            <Link key={config.id} to={`/operational-settings/${config.id}`}>
              <Card className="group border-2 hover:border-primary/50 hover:shadow-lg transition-all h-full cursor-pointer">
                <CardHeader>
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div className="p-2 rounded-lg bg-primary/10 group-hover:bg-primary/20 transition-colors">
                      <Settings className="h-5 w-5 text-primary" />
                    </div>
                    <ArrowRight className="h-5 w-5 text-muted-foreground group-hover:text-primary group-hover:translate-x-1 transition-all" />
                  </div>
                  <CardTitle className="group-hover:text-primary transition-colors line-clamp-2">
                    {config.name}
                  </CardTitle>
                  {config.description && (
                    <CardDescription className="line-clamp-2 mt-2">
                      {config.description}
                    </CardDescription>
                  )}
                </CardHeader>
                <CardContent className="space-y-3">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm text-muted-foreground">Behavior Models:</span>
                    <Badge variant="secondary">
                      {config.usageProfile?.length ?? 0}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm text-muted-foreground">Load Levels:</span>
                    <Badge variant="secondary">
                      {config.operationalProfile ? Object.keys(config.operationalProfile).length : 0}
                    </Badge>
                  </div>
                  {config.createdAt && (
                    <div className="flex items-center gap-2 text-sm text-muted-foreground pt-1 border-t">
                      <Calendar className="h-3.5 w-3.5" />
                      <span>
                        {formatDistance(new Date(config.createdAt), new Date(), { addSuffix: true })}
                      </span>
                    </div>
                  )}
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
