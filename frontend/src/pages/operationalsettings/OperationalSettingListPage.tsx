import { Link } from 'react-router-dom'
import { useListOperationalSettings, useDeleteOperationalSetting } from '@/api/generated/operational-setting-controller/operational-setting-controller'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Loader2, Plus, Settings, Trash2, FileJson } from 'lucide-react'
import { useToast } from '@/components/ui/use-toast'
import { formatDistanceToNow } from 'date-fns'

export function OperationalSettingListPage() {
  const { data, isLoading, refetch } = useListOperationalSettings()
  const deleteOperationalSetting = useDeleteOperationalSetting()
  const { toast } = useToast()

  const handleDelete = async (id: string, name: string) => {
    if (!confirm(`Are you sure you want to delete the operational setting "${name}"?`)) {
      return
    }

    try {
      await deleteOperationalSetting.mutateAsync({ operationalSettingId: id })
      toast({
        title: 'Config deleted',
        description: `Operational setting "${name}" has been deleted`,
      })
      refetch()
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to delete config',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const configs = (data as any)?.operationalSettings || []

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Operational Settings</h1>
          <p className="text-muted-foreground">Reusable load test configurations</p>
        </div>
        <Link to="/operational-settings/new">
          <Button>
            <Plus className="h-4 w-4 mr-2" />
            New Config
          </Button>
        </Link>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : configs.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Settings className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-lg font-medium mb-2">No operational settings yet</p>
            <p className="text-sm text-muted-foreground mb-4">Create reusable load test configurations</p>
            <Link to="/operational-settings/new">
              <Button>
                <Plus className="h-4 w-4 mr-2" />
                Create your first config
              </Button>
            </Link>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {configs.map((config: any) => (
            <Card key={config.id} className="hover:shadow-lg transition-shadow">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1 min-w-0">
                    <CardTitle className="truncate">{config.name}</CardTitle>
                    <CardDescription className="line-clamp-2 mt-1">
                      {config.description || 'No description'}
                    </CardDescription>
                  </div>
                  <Settings className="h-5 w-5 text-muted-foreground flex-shrink-0 ml-2" />
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2 text-sm">
                  <div className="flex items-center gap-2">
                    <FileJson className="h-4 w-4 text-muted-foreground" />
                    <span className="text-muted-foreground">OpenAPI:</span>
                    <span className="font-medium truncate">{config.openApiFile?.filename || 'N/A'}</span>
                  </div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-muted-foreground">Behavior Models:</span>
                    <Badge variant="secondary">
                      {config.usageProfile?.length ?? 0}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-muted-foreground">Load Levels:</span>
                    <Badge variant="secondary">
                      {config.operationalProfile ? Object.keys(config.operationalProfile).length : 0}
                    </Badge>
                  </div>
                  {config.createdAt && (
                    <div className="text-xs text-muted-foreground pt-2 border-t">
                      Created {formatDistanceToNow(new Date(config.createdAt), { addSuffix: true })}
                    </div>
                  )}
                </div>

                <div className="flex gap-2 pt-2">
                  <Link to={`/operational-settings/${config.id}`} className="flex-1">
                    <Button variant="outline" size="sm" className="w-full">
                      View Details
                    </Button>
                  </Link>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleDelete(config.id, config.name)}
                    disabled={deleteOperationalSetting.isPending}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
