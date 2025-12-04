import { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useGetOperationalSetting, useDeleteOperationalSetting } from '@/api/generated/operational-setting-controller/operational-setting-controller'
import type { BehaviorModel, ApiRequest } from '@/api/generated/openAPIDefinition.schemas'
import { useAuth } from '@/contexts/AuthContext'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Loader2, ArrowLeft, Trash2, FileJson, Activity, Users, ChevronDown, ChevronRight } from 'lucide-react'
import { useToast } from '@/components/ui/use-toast'
import { formatDistanceToNow } from 'date-fns'

export function OperationalSettingDetailPage() {
  const { configId } = useParams<{ configId: string }>()
  const navigate = useNavigate()
  const { data: config, isLoading } = useGetOperationalSetting(configId!)
  const deleteOperationalSetting = useDeleteOperationalSetting()
  const { toast } = useToast()
  const { user, authRequired } = useAuth()
  const [expandedSteps, setExpandedSteps] = useState<Record<string, boolean>>({})

  // Show delete button only if auth is not required OR user is authenticated
  const canDelete = !authRequired || user

  const handleDelete = async () => {
    if (!config || !confirm(`Are you sure you want to delete the operational setting "${config.name}"?`)) {
      return
    }

    try {
      await deleteOperationalSetting.mutateAsync({ operationalSettingId: config.id })
      toast({
        title: 'Config deleted',
        description: `Operational setting "${config.name}" has been deleted`,
      })
      navigate('/operational-settings')
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to delete config',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  const toggleStepExpanded = (modelIdx: number, stepIdx: number) => {
    const key = `${modelIdx}-${stepIdx}`
    setExpandedSteps(prev => ({ ...prev, [key]: !prev[key] }))
  }

  const isStepExpanded = (modelIdx: number, stepIdx: number) => {
    const key = `${modelIdx}-${stepIdx}`
    return expandedSteps[key] || false
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (!config) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Link to="/operational-settings">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <h1 className="text-3xl font-bold">Config Not Found</h1>
          </div>
        </div>
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">The operational setting you're looking for doesn't exist.</p>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link to="/operational-settings">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-4 w-4" />
            </Button>
          </Link>
          <div>
            <h1 className="text-3xl font-bold">{config.name}</h1>
            <p className="text-muted-foreground">{config.description || 'No description'}</p>
          </div>
        </div>
        {canDelete && (
          <Button variant="destructive" onClick={handleDelete} disabled={deleteOperationalSetting.isPending}>
            <Trash2 className="h-4 w-4 mr-2" />
            Delete
          </Button>
        )}
      </div>

      {/* Overview Card */}
      <Card>
        <CardHeader>
          <CardTitle>Overview</CardTitle>
          <CardDescription>Configuration details</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <FileJson className="h-4 w-4" />
                OpenAPI File
              </div>
              <p className="font-medium">{config.openApiFile?.filename || 'N/A'}</p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4 pt-4 border-t">
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Created</p>
              <p className="font-medium">
                {formatDistanceToNow(new Date(config.createdAt), { addSuffix: true })}
              </p>
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Last Updated</p>
              <p className="font-medium">
                {formatDistanceToNow(new Date(config.updatedAt), { addSuffix: true })}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Behavior Models Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Activity className="h-5 w-5" />
                Behavior Models
              </CardTitle>
              <CardDescription>User behavior patterns for load testing</CardDescription>
            </div>
            <Badge variant="secondary">{config.usageProfile?.length || 0} models</Badge>
          </div>
        </CardHeader>
        <CardContent>
          {!config.usageProfile || config.usageProfile.length === 0 ? (
            <p className="text-sm text-muted-foreground italic">No behavior models defined (will be auto-generated)</p>
          ) : (
            <div className="space-y-4">
              {config.usageProfile.map((model: BehaviorModel, idx: number) => (
                <Card key={idx} className="bg-muted/30">
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle className="text-lg">{model.id}</CardTitle>
                      <Badge>{(model.frequency * 100).toFixed(0)}% usage</Badge>
                    </div>
                    <CardDescription>Actor: {model.actor}</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div className="text-sm">
                      <div>
                        <span className="text-muted-foreground">Steps:</span>{' '}
                        <span className="font-medium">{model.steps?.length || 0}</span>
                      </div>
                    </div>
                    {model.steps && model.steps.length > 0 && (
                      <div className="space-y-2 pt-2 border-t">
                        <p className="text-sm font-medium">Request Steps:</p>
                        {model.steps.map((step: ApiRequest, stepIdx: number) => {
                          const expanded = isStepExpanded(idx, stepIdx)
                          const hasHeaders = step.headers && Object.keys(step.headers).length > 0
                          const hasParams = step.params && Object.keys(step.params).length > 0
                          const hasBody = step.body && Object.keys(step.body).length > 0
                          const hasSave = step.save && Object.keys(step.save).length > 0
                          const hasDetails = hasHeaders || hasParams || hasBody || hasSave

                          return (
                            <div key={stepIdx} className="rounded bg-background border">
                              <div
                                className={`flex items-center gap-2 text-sm p-2 transition-colors ${hasDetails ? 'cursor-pointer hover:bg-muted/50' : ''}`}
                                onClick={() => hasDetails && toggleStepExpanded(idx, stepIdx)}
                              >
                                <div className="w-4 h-4 flex-shrink-0">
                                  {hasDetails && (
                                    expanded ? (
                                      <ChevronDown className="h-4 w-4 text-muted-foreground" />
                                    ) : (
                                      <ChevronRight className="h-4 w-4 text-muted-foreground" />
                                    )
                                  )}
                                </div>
                                <Badge variant="outline" className="font-mono text-xs">{step.method}</Badge>
                                <span className="font-mono text-xs flex-1">{step.path}</span>
                                <span className="text-xs text-muted-foreground">{step.operationId}</span>
                              </div>

                              {expanded && hasDetails && (
                                <div className="px-2 pb-2 space-y-3 border-t">
                                  {hasHeaders && (
                                    <div className="pt-2">
                                      <p className="text-xs font-semibold text-muted-foreground mb-1">Headers</p>
                                      <div className="space-y-1">
                                        {Object.entries(step.headers).map(([key, value]) => (
                                          <div key={key} className="flex items-start gap-2 text-xs p-1.5 rounded bg-muted/50">
                                            <span className="font-mono font-medium min-w-[100px]">{key}:</span>
                                            <span className="font-mono text-muted-foreground break-all">{String(value)}</span>
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  )}

                                  {hasParams && (
                                    <div>
                                      <p className="text-xs font-semibold text-muted-foreground mb-1">Query Parameters</p>
                                      <div className="space-y-1">
                                        {Object.entries(step.params).map(([key, value]) => (
                                          <div key={key} className="flex items-start gap-2 text-xs p-1.5 rounded bg-muted/50">
                                            <span className="font-mono font-medium min-w-[100px]">{key}:</span>
                                            <span className="font-mono text-muted-foreground break-all">{String(value)}</span>
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  )}

                                  {hasBody && (
                                    <div>
                                      <p className="text-xs font-semibold text-muted-foreground mb-1">Request Body</p>
                                      <pre className="text-xs font-mono p-2 rounded bg-muted/50 overflow-x-auto">
                                        {JSON.stringify(step.body, null, 2)}
                                      </pre>
                                    </div>
                                  )}

                                  {hasSave && (
                                    <div>
                                      <p className="text-xs font-semibold text-muted-foreground mb-1">Save Variables</p>
                                      <div className="space-y-1">
                                        {Object.entries(step.save).map(([key, value]) => (
                                          <div key={key} className="flex items-start gap-2 text-xs p-1.5 rounded bg-muted/50">
                                            <span className="font-mono font-medium min-w-[100px]">{key}:</span>
                                            <span className="font-mono text-muted-foreground break-all">{String(value)}</span>
                                          </div>
                                        ))}
                                      </div>
                                    </div>
                                  )}
                                </div>
                              )}
                            </div>
                          )
                        })}
                      </div>
                    )}
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Operational Profile Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Users className="h-5 w-5" />
                Operational Profile
              </CardTitle>
              <CardDescription>Load distribution across different user levels</CardDescription>
            </div>
            <Badge variant="secondary">{Object.keys(config.operationalProfile || {}).length} load levels</Badge>
          </div>
        </CardHeader>
        <CardContent>
          {!config.operationalProfile || Object.keys(config.operationalProfile).length === 0 ? (
            <p className="text-sm text-muted-foreground italic">No operational profile defined</p>
          ) : (
            <div className="space-y-3">
              {Object.entries(config.operationalProfile).map(([load, frequency], idx) => (
                <div key={idx} className="flex items-center gap-4 p-3 rounded-lg bg-muted/50">
                  <div className="flex-1">
                    <div className="flex items-baseline gap-2">
                      <span className="text-2xl font-bold">{load}</span>
                      <span className="text-sm text-muted-foreground">users</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="h-2 w-32 bg-background rounded-full overflow-hidden">
                      <div
                        className="h-full bg-primary"
                        style={{ width: `${Number(frequency) * 100}%` }}
                      />
                    </div>
                    <span className="text-sm font-medium min-w-[3rem] text-right">
                      {(Number(frequency) * 100).toFixed(1)}%
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
