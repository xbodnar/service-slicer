import { useState, useEffect } from 'react'
import { useNavigate, Link, useSearchParams } from 'react-router-dom'
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { FileSelector } from '@/components/ui/file-selector'
import { useToast } from '@/components/ui/use-toast'
import { type UploadedFile } from '@/hooks/useFileUpload'
import { useCreateOperationalSetting, useGetOperationalSetting } from '@/api/generated/operational-setting-controller/operational-setting-controller'
import { useListApiOperations, useCreateApiOperations } from '@/api/generated/api-operation-controller/api-operation-controller'
import { type ApiOperation } from '@/api/generated/openAPIDefinition.schemas'
import { OperationAutocomplete } from '@/components/ui/operation-autocomplete'
import { AutocompleteInput } from '@/components/ui/autocomplete-input'
import { ArrowLeft, Plus, Trash2, Check, Activity, FileJson, Loader2, ChevronDown, ChevronRight } from 'lucide-react'

type BehaviorModelInputMode = 'manual' | 'json' | 'auto-generate'

// Common HTTP headers for autocomplete
const COMMON_HTTP_HEADERS = [
  'Accept',
  'Accept-Encoding',
  'Accept-Language',
  'Authorization',
  'Cache-Control',
  'Content-Type',
  'Cookie',
  'Origin',
  'Referer',
  'User-Agent',
  'X-API-Key',
  'X-Auth-Token',
  'X-Requested-With',
  'X-CSRF-Token',
  'If-None-Match',
  'If-Modified-Since',
]

// Form schemas (reusing from BenchmarkCreatePage)
const keyValuePairSchema = z.object({
  key: z.string(),
  value: z.string(),
})

const behaviorModelSchema = z.object({
  id: z.string().min(1, 'ID is required'),
  actor: z.string().min(1, 'Actor name is required'),
  frequency: z.coerce.number().min(0).max(1),
  commonHeaders: z.array(keyValuePairSchema).default([]), // Not sent to backend
  steps: z.array(
    z.object({
      method: z.string().min(1, 'Method is required'),
      path: z.string().min(1, 'Path is required'),
      operationId: z.string().min(1, 'Operation ID is required'),
      component: z.string().optional(),
      waitMsFrom: z.coerce.number().min(0, 'Wait from must be non-negative'),
      waitMsTo: z.coerce.number().min(0, 'Wait to must be non-negative'),
      headers: z.array(keyValuePairSchema).default([]),
      params: z.array(keyValuePairSchema).default([]),
      body: z.string().default('{}'),
      save: z.array(keyValuePairSchema).default([]),
    })
  ).default([]),
})

const benchmarkConfigSchema = z.object({
  name: z.string().min(1, 'Config name is required'),
  description: z.string().optional(),
  generateBehaviorModels: z.boolean().default(false),
  behaviorModels: z.array(behaviorModelSchema).optional(),
  behaviorModelsJson: z.string().optional(),
  operationalProfile: z.array(
    z.object({
      load: z.coerce.number().min(1, 'Load must be at least 1'),
      frequency: z.coerce.number().min(0).max(1, 'Frequency must be between 0 and 1'),
    })
  ).min(1, 'At least one operational load is required'),
}).refine((data) => {
  if (data.generateBehaviorModels) return true
  if (data.behaviorModels && data.behaviorModels.length > 0) return true
  if (data.behaviorModelsJson && data.behaviorModelsJson.trim() !== '') return true
  return false
}, {
  message: 'Either enable auto-generation, provide JSON, or define at least one behavior model manually',
  path: ['behaviorModels'],
})

type BenchmarkConfigFormData = z.infer<typeof benchmarkConfigSchema>

interface KeyValuePairListProps {
  name: string
  control: any
  label: string
  keyPlaceholder?: string
  valuePlaceholder?: string
  keyAutocomplete?: string[]
  watch?: any
  setValue?: any
}

function KeyValuePairList({ name, control, label, keyPlaceholder = 'Key', valuePlaceholder = 'Value', keyAutocomplete, watch, setValue }: KeyValuePairListProps) {
  const { fields, append, remove } = useFieldArray({
    control,
    name,
  })

  // Get current form values if watch is provided
  const getFieldValue = (index: number, field: 'key' | 'value') => {
    if (!watch) return ''
    const pathParts = name.split('.')
    let currentValue = watch()
    for (const part of pathParts) {
      if (currentValue && typeof currentValue === 'object') {
        currentValue = currentValue[part]
      }
    }
    return currentValue?.[index]?.[field] || ''
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Label className="text-xs">{label}</Label>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="h-7 px-2 gap-1"
          onClick={() => append({ key: '', value: '' })}
        >
          <Plus className="h-4 w-4" />
          <span className="text-xs">Add</span>
        </Button>
      </div>
      <div className="space-y-1.5">
        {fields.map((field, index) => (
          <div key={field.id} className="flex items-center gap-1.5">
            {keyAutocomplete && watch && setValue ? (
              <AutocompleteInput
                value={getFieldValue(index, 'key')}
                onChange={(val) => setValue(`${name}.${index}.key`, val)}
                suggestions={keyAutocomplete}
                placeholder={keyPlaceholder}
                className="h-7 text-xs"
              />
            ) : (
              <Input
                {...control.register(`${name}.${index}.key`)}
                placeholder={keyPlaceholder}
                className="h-7 text-xs flex-1"
              />
            )}
            <Input
              {...control.register(`${name}.${index}.value`)}
              placeholder={valuePlaceholder}
              className="h-7 text-xs flex-1"
            />
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-7 w-7 p-0"
              onClick={() => remove(index)}
            >
              <Trash2 className="h-3 w-3" />
            </Button>
          </div>
        ))}
        {fields.length === 0 && (
          <p className="text-xs text-muted-foreground italic">No {label.toLowerCase()} defined</p>
        )}
      </div>
    </div>
  )
}

interface BehaviorModelStepsProps {
  behaviorIndex: number
  control: any
  register: any
  errors: any
  apiOperations: ApiOperation[]
  setValue: any
  watch: any
}

function BehaviorModelSteps({ behaviorIndex, control, register, errors, apiOperations, setValue, watch }: BehaviorModelStepsProps) {
  const { fields: stepFields, append: appendStep, remove: removeStep } = useFieldArray({
    control,
    name: `behaviorModels.${behaviorIndex}.steps`,
  })

  const behaviorModels = watch('behaviorModels') || []

  return (
    <div className="space-y-3">
      <Label>API Request Steps</Label>

      {stepFields.map((field: any, stepIndex) => (
        <Card key={field.id} className="p-3 bg-muted/30">
          <div className="space-y-3">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium">Step {stepIndex + 1}</span>
              <Button type="button" variant="ghost" size="sm" onClick={() => removeStep(stepIndex)}>
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>

            <div className="grid grid-cols-[2fr_1fr_2fr] gap-3">
              <div className="space-y-2">
                <Label>Operation ID</Label>
                <OperationAutocomplete
                  value={behaviorModels[behaviorIndex]?.steps?.[stepIndex]?.operationId || ''}
                  onChange={(value) => setValue(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.operationId`, value)}
                  onSelectOperation={(operation) => {
                    setValue(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.operationId`, operation.operationId)
                    setValue(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.method`, operation.method)
                    setValue(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.path`, operation.path)
                  }}
                  operations={apiOperations}
                  placeholder="getUser or start typing..."
                  error={errors?.behaviorModels?.[behaviorIndex]?.steps?.[stepIndex]?.operationId?.message}
                />
              </div>

              <div className="space-y-2">
                <Label>Method</Label>
                <Select
                  value={behaviorModels[behaviorIndex]?.steps?.[stepIndex]?.method || 'GET'}
                  onValueChange={(value) => setValue(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.method`, value)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select method" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="GET">GET</SelectItem>
                    <SelectItem value="POST">POST</SelectItem>
                    <SelectItem value="PUT">PUT</SelectItem>
                    <SelectItem value="PATCH">PATCH</SelectItem>
                    <SelectItem value="DELETE">DELETE</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Path</Label>
                <Input
                  {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.path`)}
                  placeholder="/api/resource"
                />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-2">
                <Label>Component</Label>
                <Input
                  {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.component`)}
                  placeholder="user-service"
                />
              </div>
              <div className="space-y-2">
                <Label>Wait From (ms)</Label>
                <Input
                  type="number"
                  {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.waitMsFrom`)}
                  placeholder="0"
                />
              </div>
              <div className="space-y-2">
                <Label>Wait To (ms)</Label>
                <Input
                  type="number"
                  {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.waitMsTo`)}
                  placeholder="0"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <KeyValuePairList
                name={`behaviorModels.${behaviorIndex}.steps.${stepIndex}.headers`}
                control={control}
                label="Headers"
                keyPlaceholder="Header name"
                valuePlaceholder="Header value"
                keyAutocomplete={COMMON_HTTP_HEADERS}
                watch={watch}
                setValue={setValue}
              />
              <KeyValuePairList
                name={`behaviorModels.${behaviorIndex}.steps.${stepIndex}.params`}
                control={control}
                label="Query Params"
                keyPlaceholder="Param name"
                valuePlaceholder="Param value"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label className="text-xs">Body (JSON)</Label>
                <Textarea
                  {...register(`behaviorModels.${behaviorIndex}.steps.${stepIndex}.body`)}
                  placeholder='{"key": "value"}'
                  rows={4}
                  className="font-mono text-xs"
                />
              </div>
              <KeyValuePairList
                name={`behaviorModels.${behaviorIndex}.steps.${stepIndex}.save`}
                control={control}
                label="Save Fields"
                keyPlaceholder="Variable name"
                valuePlaceholder="JSONPath ($.id)"
              />
            </div>
          </div>
        </Card>
      ))}

      <div className="flex justify-end">
        <Button
          type="button"
          variant="secondary"
          size="sm"
          onClick={() => {
            const commonHeaders = behaviorModels[behaviorIndex]?.commonHeaders || []
            appendStep({
              method: 'GET',
              path: '/',
              operationId: '',
              component: '',
              waitMsFrom: 0,
              waitMsTo: 0,
              headers: [...commonHeaders],
              params: [],
              body: '{}',
              save: []
            })
          }}
        >
          <Plus className="h-4 w-4 mr-2" />
          Add Step
        </Button>
      </div>
    </div>
  )
}

export function OperationalSettingCreatePage() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const [searchParams] = useSearchParams()
  const duplicateId = searchParams.get('duplicate')

  const createOperationalSetting = useCreateOperationalSetting()
  const createApiOperationsMutation = useCreateApiOperations()

  const [openApiFile, setOpenApiFile] = useState<UploadedFile | null>(null)
  const [behaviorModelInputMode, setBehaviorModelInputMode] = useState<BehaviorModelInputMode>('manual')
  const [jsonValidationError, setJsonValidationError] = useState<string | null>(null)
  const [apiOperations, setApiOperations] = useState<ApiOperation[]>([])
  const [fetchingOperations, setFetchingOperations] = useState(false)
  const [actors, setActors] = useState<string[]>([])
  const [newActor, setNewActor] = useState('')
  const [collapsedModels, setCollapsedModels] = useState<Set<number>>(new Set())
  const [isDuplicating, setIsDuplicating] = useState(false)

  // Fetch the operational setting to duplicate
  const { data: duplicateConfig } = useGetOperationalSetting(duplicateId || '', {
    query: {
      enabled: !!duplicateId,
    }
  })

  // Fetch API operations when openApiFile changes
  const { refetch: refetchApiOperations } = useListApiOperations(
    { openApiFileId: openApiFile?.fileId || '' },
    {
      query: {
        enabled: false, // Manual fetch
      }
    }
  )

  useEffect(() => {
    const fetchApiOperations = async () => {
      if (!openApiFile?.fileId) {
        setApiOperations([])
        return
      }

      setFetchingOperations(true)
      try {
        // First, try to list existing operations
        const { data: existingOps } = await refetchApiOperations()

        if (existingOps && existingOps.length > 0) {
          setApiOperations(existingOps)
        } else {
          // If no operations exist, create them
          const newOps = await createApiOperationsMutation.mutateAsync({
            params: { openApiFileId: openApiFile.fileId }
          })
          setApiOperations(newOps)
        }
      } catch (error) {
        console.error('Failed to fetch API operations:', error)
        toast({
          variant: 'destructive',
          title: 'Failed to fetch API operations',
          description: error instanceof Error ? error.message : 'An unknown error occurred',
        })
        setApiOperations([])
      } finally {
        setFetchingOperations(false)
      }
    }

    fetchApiOperations()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [openApiFile?.fileId])

  // Pre-fill form when duplicating
  useEffect(() => {
    if (duplicateConfig && !isDuplicating) {
      setIsDuplicating(true)

      // Set OpenAPI file
      if (duplicateConfig.openApiFile) {
        setOpenApiFile({
          fileId: duplicateConfig.openApiFile.id,
          filename: duplicateConfig.openApiFile.filename,
          size: duplicateConfig.openApiFile.fileSize,
        })
      }

      // Convert operational profile from object to array
      const operationalProfileArray = duplicateConfig.operationalProfile
        ? Object.entries(duplicateConfig.operationalProfile).map(([load, frequency]) => ({
            load: Number(load),
            frequency: Number(frequency),
          }))
        : [{ load: 25, frequency: 1.0 }]

      // Pre-fill form values
      form.setValue('name', `${duplicateConfig.name} (Copy)`)
      form.setValue('description', duplicateConfig.description || '')
      form.setValue('operationalProfile', operationalProfileArray)

      // Set behavior models
      if (duplicateConfig.usageProfile && duplicateConfig.usageProfile.length > 0) {
        // Extract unique actors
        const uniqueActors = Array.from(new Set(duplicateConfig.usageProfile.map((m: any) => m.actor).filter(Boolean)))
        setActors(uniqueActors)

        // Convert to manual format
        const manualModels = duplicateConfig.usageProfile.map((model: any) => ({
          id: model.id || '',
          actor: model.actor || '',
          frequency: model.frequency || 0.5,
          commonHeaders: [],
          steps: (model.steps || []).map((step: any) => ({
            method: step.method || 'GET',
            path: step.path || '/',
            operationId: step.operationId || '',
            component: step.component || '',
            waitMsFrom: step.waitMsFrom || 0,
            waitMsTo: step.waitMsTo || 0,
            headers: objectToKeyValuePairs(step.headers),
            params: objectToKeyValuePairs(step.params),
            body: typeof step.body === 'string' ? step.body : JSON.stringify(step.body || {}, null, 2),
            save: objectToKeyValuePairs(step.save),
          }))
        }))

        form.setValue('behaviorModels', manualModels)
        setBehaviorModelInputMode('manual')
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [duplicateConfig])

  const form = useForm<BenchmarkConfigFormData>({
    resolver: zodResolver(benchmarkConfigSchema),
    defaultValues: {
      name: '',
      description: '',
      generateBehaviorModels: false,
      behaviorModels: [],
      behaviorModelsJson: '',
      operationalProfile: [{ load: 25, frequency: 1.0 }],
    },
  })

  const { fields: behaviorFields, append: appendBehavior, remove: removeBehavior } = useFieldArray({
    control: form.control,
    name: 'behaviorModels',
  })

  const { fields: operationalProfileFields, append: appendOperationalProfile, remove: removeOperationalProfile } = useFieldArray({
    control: form.control,
    name: 'operationalProfile',
  })

  const keyValuePairsToObject = (pairs: { key: string; value: string }[]): Record<string, string> => {
    return pairs.reduce((acc, { key, value }) => {
      if (key.trim()) {
        acc[key] = value
      }
      return acc
    }, {} as Record<string, string>)
  }

  const objectToKeyValuePairs = (obj: Record<string, string> | undefined): { key: string; value: string }[] => {
    if (!obj) return []
    return Object.entries(obj).map(([key, value]) => ({ key, value }))
  }

  // Convert manual models to JSON format
  const convertManualToJson = (models: any[]): string => {
    if (!models || models.length === 0) return ''

    const jsonModels = models.map((model: any) => ({
      id: model.id,
      actor: model.actor,
      frequency: model.frequency,
      steps: (model.steps || []).map((step: any) => ({
        method: step.method,
        path: step.path,
        operationId: step.operationId,
        ...(step.component && { component: step.component }),
        waitMsFrom: Number(step.waitMsFrom) || 0,
        waitMsTo: Number(step.waitMsTo) || 0,
        headers: keyValuePairsToObject(step.headers || []),
        params: keyValuePairsToObject(step.params || []),
        body: step.body && step.body.trim() !== '' ? (typeof step.body === 'string' ? JSON.parse(step.body) : step.body) : {},
        save: keyValuePairsToObject(step.save || []),
      }))
    }))

    return JSON.stringify(jsonModels, null, 2)
  }

  // Convert JSON to manual models format
  const convertJsonToManual = (jsonString: string): any[] => {
    if (!jsonString || jsonString.trim() === '') return []

    try {
      const parsed = JSON.parse(jsonString)
      if (!Array.isArray(parsed)) return []

      return parsed.map((model: any) => ({
        id: model.id || '',
        actor: model.actor || '',
        frequency: model.frequency || 0.5,
        commonHeaders: [], // Common headers are not stored in JSON
        steps: (model.steps || []).map((step: any) => ({
          method: step.method || 'GET',
          path: step.path || '/',
          operationId: step.operationId || '',
          component: step.component || '',
          waitMsFrom: step.waitMsFrom || 0,
          waitMsTo: step.waitMsTo || 0,
          headers: objectToKeyValuePairs(step.headers),
          params: objectToKeyValuePairs(step.params),
          body: typeof step.body === 'string' ? step.body : JSON.stringify(step.body || {}, null, 2),
          save: objectToKeyValuePairs(step.save),
        }))
      }))
    } catch (e) {
      console.error('Failed to convert JSON to manual format:', e)
      return []
    }
  }

  const validateBehaviorModelsJson = (jsonString: string) => {
    if (!jsonString || jsonString.trim() === '') {
      setJsonValidationError(null)
      return
    }

    try {
      const parsed = JSON.parse(jsonString)
      if (!Array.isArray(parsed)) {
        setJsonValidationError('Behavior models must be an array')
        return
      }

      for (let i = 0; i < parsed.length; i++) {
        const model = parsed[i]
        if (!model.id || !model.actor || typeof model.frequency !== 'number') {
          setJsonValidationError(`Model at index ${i} is missing required fields`)
          return
        }
        if (!Array.isArray(model.steps) || model.steps.length === 0) {
          setJsonValidationError(`Model at index ${i} must have at least one step`)
          return
        }
        for (let j = 0; j < model.steps.length; j++) {
          const step = model.steps[j]
          if (!step.method || !step.path || !step.operationId) {
            setJsonValidationError(`Model ${model.id}, step ${j}: missing required fields`)
            return
          }
        }
      }

      setJsonValidationError(null)
    } catch (e) {
      if (e instanceof SyntaxError) {
        setJsonValidationError(`Invalid JSON syntax: ${e.message}`)
      } else {
        setJsonValidationError(`Validation error: ${e instanceof Error ? e.message : String(e)}`)
      }
    }
  }

  const onSubmit = async (data: BenchmarkConfigFormData) => {
    if (!openApiFile) {
      toast({
        variant: 'destructive',
        title: 'OpenAPI file is required',
      })
      return
    }

    if (behaviorModelInputMode === 'json' && jsonValidationError) {
      toast({
        variant: 'destructive',
        title: 'Invalid behavior models JSON',
        description: jsonValidationError,
      })
      return
    }

    try {
      let behaviorModels: any[] = []

      if (behaviorModelInputMode === 'json' && data.behaviorModelsJson) {
        behaviorModels = JSON.parse(data.behaviorModelsJson)
      } else if (behaviorModelInputMode === 'manual' && data.behaviorModels && data.behaviorModels.length > 0) {
        // Validate that all behavior models have at least one step
        const modelsWithoutSteps = data.behaviorModels.filter((m: any) => !m.steps || m.steps.length === 0)
        if (modelsWithoutSteps.length > 0) {
          toast({
            variant: 'destructive',
            title: 'Incomplete behavior models',
            description: 'All behavior models must have at least one API request step',
          })
          return
        }

        behaviorModels = data.behaviorModels.map((model: any) => {
          const steps = model.steps.map((step: any) => ({
            method: step.method,
            path: step.path,
            operationId: step.operationId,
            component: step.component || undefined,
            waitMsFrom: Number(step.waitMsFrom) || 0,
            waitMsTo: Number(step.waitMsTo) || 0,
            headers: keyValuePairsToObject(step.headers || []),
            params: keyValuePairsToObject(step.params || []),
            body: step.body && step.body.trim() !== '' ? JSON.parse(step.body) : {},
            save: keyValuePairsToObject(step.save || []),
          }))
          // Exclude commonHeaders from the payload sent to backend
          return {
            id: model.id,
            actor: model.actor,
            frequency: model.frequency,
            steps,
          }
        })
      }

      const freqSum = data.operationalProfile.reduce((sum: number, p: any) => sum + Number(p.frequency), 0)
      if (Math.abs(freqSum - 1.0) > 0.001) {
        toast({
          variant: 'destructive',
          title: 'Invalid operational profile',
          description: `Frequencies must sum to 1.0 (current sum: ${freqSum.toFixed(3)})`,
        })
        return
      }

      // Transform operationalProfile from array to object
      const operationalProfile = data.operationalProfile.reduce((acc: Record<string, number>, p: any) => {
        acc[p.load.toString()] = Number(p.frequency)
        return acc
      }, {})

      const result = await createOperationalSetting.mutateAsync({
        data: {
          name: data.name,
          description: data.description || undefined,
          openApiFileId: openApiFile.fileId,
          usageProfile: behaviorModels,
          generateUsageProfile: data.generateBehaviorModels,
          operationalProfile,
        },
      })

      toast({
        title: 'Config created',
        description: 'Your benchmark config has been created successfully',
      })

      navigate(`/operational-settings/${result.id}`)
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to create config',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/operational-settings">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold">
            {duplicateId ? 'Duplicate Operational Setting' : 'Create Operational Setting'}
          </h1>
          <p className="text-muted-foreground">
            {duplicateId ? 'Create a copy of an existing load test configuration' : 'Create a reusable load test configuration'}
          </p>
        </div>
      </div>

      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        {/* Combined Card */}
        <Card>
          <CardContent className="space-y-6 pt-6">
            {/* Basic Info and OpenAPI File in same row */}
            <div className="grid grid-cols-2 gap-6">
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="name">Name *</Label>
                  <Input id="name" {...form.register('name')} placeholder="Standard Load Test" />
                  {form.formState.errors.name && (
                    <p className="text-sm text-destructive">{String(form.formState.errors.name.message)}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="description">Description (optional)</Label>
                  <Textarea
                    id="description"
                    {...form.register('description')}
                    placeholder="Standard load test configuration for REST APIs"
                    rows={3}
                  />
                </div>
              </div>

              <div className="space-y-2">
                <FileSelector
                  id="openapi-file"
                  label="OpenAPI Specification File"
                  accept=".json,.yaml,.yml"
                  required
                  onFileSelected={setOpenApiFile}
                  selectedFile={openApiFile}
                  mimeTypeFilter="json"
                />
                {fetchingOperations && (
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Loader2 className="h-3 w-3 animate-spin" />
                    <span>Loading API operations...</span>
                  </div>
                )}
                {!fetchingOperations && apiOperations.length > 0 && (
                  <div className="flex items-center gap-2 text-sm text-green-600">
                    <Check className="h-3 w-3" />
                    <span>{apiOperations.length} API operations loaded</span>
                  </div>
                )}
              </div>
            </div>

            {/* Behavior Models */}
            <div className="space-y-4">
              <div className="space-y-3">
                <Label>Behavior Models Input Method</Label>
                <div className="grid grid-cols-3 gap-3">
                    <Button
                        type="button"
                        variant={behaviorModelInputMode === 'manual' ? 'default' : 'outline'}
                        className="w-full"
                        onClick={() => {
                            // Convert JSON to manual if switching from JSON mode
                            if (behaviorModelInputMode === 'json') {
                              const currentJson = form.watch('behaviorModelsJson')
                              if (currentJson && currentJson.trim() !== '' && !jsonValidationError) {
                                const manualModels = convertJsonToManual(currentJson)
                                form.setValue('behaviorModels', manualModels)

                                // Extract and sync actors from JSON
                                try {
                                  const parsed = JSON.parse(currentJson)
                                  if (Array.isArray(parsed)) {
                                    const uniqueActors = Array.from(new Set(parsed.map((m: any) => m.actor).filter(Boolean)))
                                    setActors(uniqueActors)
                                  }
                                } catch (e) {
                                  console.error('Failed to extract actors from JSON:', e)
                                }
                              }
                            }
                            setBehaviorModelInputMode('manual')
                            form.setValue('generateBehaviorModels', false)
                            setJsonValidationError(null)
                        }}
                    >
                        Manual Input
                    </Button>
                  <Button
                    type="button"
                    variant={behaviorModelInputMode === 'json' ? 'default' : 'outline'}
                    className="w-full"
                    onClick={() => {
                      // Convert manual to JSON if switching from manual mode
                      if (behaviorModelInputMode === 'manual') {
                        const currentModels = form.watch('behaviorModels')
                        if (currentModels && currentModels.length > 0) {
                          try {
                            const jsonString = convertManualToJson(currentModels)
                            form.setValue('behaviorModelsJson', jsonString)
                            validateBehaviorModelsJson(jsonString)

                            // Extract and sync actors from manual models
                            const uniqueActors = Array.from(new Set(currentModels.map((m: any) => m.actor).filter(Boolean)))
                            setActors(uniqueActors)
                          } catch (e) {
                            console.error('Failed to convert manual to JSON:', e)
                          }
                        }
                      } else {
                        const currentJson = form.watch('behaviorModelsJson')
                        if (currentJson) validateBehaviorModelsJson(currentJson)
                      }
                      setBehaviorModelInputMode('json')
                      form.setValue('generateBehaviorModels', false)
                    }}
                  >
                    <FileJson className="h-4 w-4 mr-2" />
                    Raw JSON
                  </Button>
                    <Button
                        type="button"
                        variant={behaviorModelInputMode === 'auto-generate' ? 'default' : 'outline'}
                        className="w-full"
                        onClick={() => {
                            setBehaviorModelInputMode('auto-generate')
                            form.setValue('generateBehaviorModels', true)
                            setJsonValidationError(null)
                        }}
                    >
                        <Activity className="h-4 w-4 mr-2" />
                        Auto-generate
                    </Button>
                </div>
              </div>

              {behaviorModelInputMode === 'auto-generate' && (
                <div className="p-4 rounded-lg bg-blue-50 border border-blue-200">
                  <p className="text-sm text-blue-900">
                    Behavior models will be automatically generated from the OpenAPI specification file.
                  </p>
                </div>
              )}

              {behaviorModelInputMode === 'json' && (
                <div className="space-y-2">
                  <Label htmlFor="behavior-models-json">Behavior Models JSON</Label>
                  <Textarea
                    id="behavior-models-json"
                    {...form.register('behaviorModelsJson', {
                      onChange: (e) => validateBehaviorModelsJson(e.target.value)
                    })}
                    placeholder={`[{"id": "checkout-flow", "actor": "Customer", "usageProfile": 0.7, "steps": [{"method": "GET", "path": "/api/products", "operationId": "listProducts", "headers": {}, "params": {}, "body": {}, "save": {}}]}]`}
                    rows={10}
                    className={`font-mono text-xs ${jsonValidationError ? 'border-destructive' : ''}`}
                  />
                  {jsonValidationError && (
                    <div className="flex items-start gap-2 p-3 rounded-lg bg-destructive/10 border border-destructive">
                      <p className="text-sm text-destructive">{jsonValidationError}</p>
                    </div>
                  )}
                  {!jsonValidationError && form.watch('behaviorModelsJson') && form.watch('behaviorModelsJson')?.trim() !== '' && (
                    <div className="flex items-center gap-2 p-2 rounded-lg bg-green-50 border border-green-200">
                      <Check className="h-4 w-4 text-green-600" />
                      <p className="text-sm text-green-900">Valid JSON format</p>
                    </div>
                  )}
                </div>
              )}

              {behaviorModelInputMode === 'manual' && (
                <div className="space-y-4">
                  {/* Actors Management */}
                  <div className="space-y-3 p-4 rounded-lg bg-muted/30 border">
                    <Label>Actors</Label>
                    <p className="text-xs text-muted-foreground">
                      Define actors that will be available for behavior models
                    </p>
                    <div className="flex items-center gap-2">
                      <Input
                        value={newActor}
                        onChange={(e) => setNewActor(e.target.value)}
                        placeholder="e.g., Customer, Admin, Guest"
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') {
                            e.preventDefault()
                            if (newActor.trim() && !actors.includes(newActor.trim())) {
                              setActors([...actors, newActor.trim()])
                              setNewActor('')
                            }
                          }
                        }}
                      />
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          if (newActor.trim() && !actors.includes(newActor.trim())) {
                            setActors([...actors, newActor.trim()])
                            setNewActor('')
                          }
                        }}
                      >
                        <Plus className="h-4 w-4 mr-1" />
                        Add
                      </Button>
                    </div>
                    {actors.length > 0 && (
                      <div className="flex flex-wrap gap-2">
                        {actors.map((actor) => (
                          <div
                            key={actor}
                            className="flex items-center gap-1 px-2 py-1 bg-white border rounded-md text-sm"
                          >
                            <span>{actor}</span>
                            <button
                              type="button"
                              onClick={() => setActors(actors.filter((a) => a !== actor))}
                              className="hover:text-destructive"
                            >
                              <Trash2 className="h-3 w-3" />
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                    {actors.length === 0 && (
                      <p className="text-xs text-muted-foreground italic">
                        No actors defined yet. Add actors to use in behavior models.
                      </p>
                    )}
                  </div>

                  <div className="pt-2">
                    <Label>Behavior Models</Label>
                  </div>

                  {behaviorFields.map((field, index) => {
                    const isCollapsed = collapsedModels.has(index)
                    const currentModel = form.watch(`behaviorModels.${index}`)

                    return (
                      <Card key={field.id} className="p-4">
                        <div className="space-y-3">
                          <div className="flex items-center gap-2">
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              className="h-8 w-8 p-0"
                              onClick={() => {
                                const newCollapsed = new Set(collapsedModels)
                                if (isCollapsed) {
                                  newCollapsed.delete(index)
                                } else {
                                  newCollapsed.add(index)
                                }
                                setCollapsedModels(newCollapsed)
                              }}
                            >
                              {isCollapsed ? (
                                <ChevronRight className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              )}
                            </Button>
                            <div className="flex-1">
                              <span className="font-medium">
                                {currentModel?.actor || 'No actor'} - {currentModel?.id || 'Untitled'}
                              </span>
                            </div>
                            <Button type="button" variant="ghost" size="icon" onClick={() => removeBehavior(index)}>
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>

                          {!isCollapsed && (
                            <>
                              <div className="grid grid-cols-3 gap-3">
                                <div className="space-y-2">
                                  <Label>ID</Label>
                                  <Input {...form.register(`behaviorModels.${index}.id`)} placeholder="checkout-flow" />
                                </div>
                                <div className="space-y-2">
                                  <Label>Actor</Label>
                                  {actors.length > 0 ? (
                                    <Select
                                      value={currentModel?.actor || ''}
                                      onValueChange={(value) => form.setValue(`behaviorModels.${index}.actor`, value)}
                                    >
                                      <SelectTrigger>
                                        <SelectValue placeholder="Select actor" />
                                      </SelectTrigger>
                                      <SelectContent>
                                        {actors.map((actor) => (
                                          <SelectItem key={actor} value={actor}>
                                            {actor}
                                          </SelectItem>
                                        ))}
                                      </SelectContent>
                                    </Select>
                                  ) : (
                                    <Input {...form.register(`behaviorModels.${index}.actor`)} placeholder="Add actors above first" disabled />
                                  )}
                                </div>
                                <div className="space-y-2">
                                  <Label>Frequency (0-1)</Label>
                                  <Input type="number" step="0.01" min="0" max="1" {...form.register(`behaviorModels.${index}.frequency`)} />
                                </div>
                              </div>

                              <div className="space-y-2">
                                <KeyValuePairList
                                  name={`behaviorModels.${index}.commonHeaders`}
                                  control={form.control}
                                  label="Common Headers"
                                  keyPlaceholder="Header name"
                                  valuePlaceholder="Header value"
                                  keyAutocomplete={COMMON_HTTP_HEADERS}
                                  watch={form.watch}
                                  setValue={form.setValue}
                                />
                                <p className="text-xs text-muted-foreground italic">
                                  These headers will be automatically added to every new API request step you create
                                </p>
                              </div>

                              <BehaviorModelSteps
                                behaviorIndex={index}
                                control={form.control}
                                register={form.register}
                                errors={form.formState.errors}
                                apiOperations={apiOperations}
                                setValue={form.setValue}
                                watch={form.watch}
                              />

                              {(!currentModel?.steps || currentModel.steps.length === 0) && (
                                <div className="p-3 rounded-lg bg-amber-50 border border-amber-200">
                                  <p className="text-sm text-amber-900">
                                    ⚠️ This behavior model has no steps. Add at least one API request step before submitting.
                                  </p>
                                </div>
                              )}
                            </>
                          )}
                        </div>
                      </Card>
                    )
                  })}

                  {behaviorFields.length === 0 && (
                    <p className="text-sm text-muted-foreground italic">
                      No behavior models defined. Click "Add Model" to create one.
                    </p>
                  )}

                  {form.formState.errors.behaviorModels && (
                    <p className="text-sm text-destructive">{form.formState.errors.behaviorModels.message}</p>
                  )}

                  <div className="pt-4 mt-4 border-t border-gray-200">
                    <div className="flex justify-end">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => appendBehavior({
                          id: '',
                          actor: '',
                          frequency: 0.5,
                          commonHeaders: [],
                          steps: [],
                        })}
                      >
                        <Plus className="h-4 w-4 mr-2" />
                        Add Model
                      </Button>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Operational Profile */}
            <div className="space-y-4">
              <Label>Operational Profile</Label>

              {operationalProfileFields.map((field, index) => (
                <div key={field.id} className="flex items-center gap-4 p-3 rounded-lg bg-muted/50">
                  <div className="flex-1 grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label>Load (users)</Label>
                      <Input type="number" {...form.register(`operationalProfile.${index}.load`)} />
                    </div>
                    <div className="space-y-2">
                      <Label>Frequency (0-1)</Label>
                      <Input type="number" step="0.001" min="0" max="1" {...form.register(`operationalProfile.${index}.frequency`)} />
                    </div>
                  </div>
                  {operationalProfileFields.length > 1 && (
                    <Button type="button" variant="ghost" size="icon" onClick={() => removeOperationalProfile(index)}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  )}
                </div>
              ))}

              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-muted-foreground">Total frequency:</span>
                  <span className={`text-sm font-medium ${
                    (() => {
                      const sum = form.watch('operationalProfile')?.reduce((acc: number, p: any) => acc + Number(p.frequency || 0), 0) || 0
                      return Math.abs(sum - 1.0) < 0.001 ? 'text-green-600' : 'text-orange-600'
                    })()
                  }`}>
                    {(form.watch('operationalProfile')?.reduce((acc: number, p: any) => acc + Number(p.frequency || 0), 0) || 0).toFixed(3)}
                  </span>
                  {(() => {
                    const sum = form.watch('operationalProfile')?.reduce((acc: number, p: any) => acc + Number(p.frequency || 0), 0) || 0
                    return Math.abs(sum - 1.0) < 0.001 ? (
                      <Check className="h-4 w-4 text-green-600" />
                    ) : (
                      <span className="text-xs text-orange-600">(must sum to 1.0)</span>
                    )
                  })()}
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => appendOperationalProfile({ load: 50, frequency: 0 })}
                >
                  <Plus className="h-4 w-4 mr-2" />
                  Add Load
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="flex justify-end gap-4">
          <Link to="/operational-settings">
            <Button type="button" variant="outline">Cancel</Button>
          </Link>
          <Button type="submit" disabled={createOperationalSetting.isPending}>
            {createOperationalSetting.isPending ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Creating...
              </>
            ) : (
              'Create Config'
            )}
          </Button>
        </div>
      </form>
    </div>
  )
}
