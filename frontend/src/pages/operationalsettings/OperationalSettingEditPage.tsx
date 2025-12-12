import { useState, useEffect } from 'react'
import { useNavigate, Link, useParams } from 'react-router-dom'
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useToast } from '@/components/ui/use-toast'
import { useGetOperationalSetting, useUpdateOperationalSetting } from '@/api/generated/operational-setting-controller/operational-setting-controller'
import { useListApiOperations } from '@/api/generated/api-operation-controller/api-operation-controller'
import { type ApiOperation } from '@/api/generated/openAPIDefinition.schemas'
import { ArrowLeft, Plus, Trash2, Check, FileJson, Loader2, ChevronDown, ChevronRight } from 'lucide-react'

type BehaviorModelInputMode = 'manual' | 'json'

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

// Form schemas
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
  ).min(1, 'At least one step is required').default([]),
})

const editConfigSchema = z.object({
  name: z.string().min(1, 'Config name is required'),
  description: z.string().optional(),
  behaviorModels: z.array(behaviorModelSchema).optional(),
  behaviorModelsJson: z.string().optional(),
  operationalProfile: z.array(
    z.object({
      load: z.coerce.number().min(1, 'Load must be at least 1'),
      frequency: z.coerce.number().min(0).max(1, 'Frequency must be between 0 and 1'),
    })
  ).min(1, 'At least one operational load is required'),
}).refine((data) => {
  if (data.behaviorModels && data.behaviorModels.length > 0) return true
  if (data.behaviorModelsJson && data.behaviorModelsJson.trim() !== '') return true
  return false
}, {
  message: 'Either provide JSON or define at least one behavior model manually',
  path: ['behaviorModels'],
})

type EditConfigFormData = z.infer<typeof editConfigSchema>

interface AutocompleteInputProps {
  value: string
  onChange: (value: string) => void
  suggestions: string[]
  placeholder?: string
  className?: string
}

function AutocompleteInput({ value, onChange, suggestions, placeholder, className }: AutocompleteInputProps) {
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [filteredSuggestions, setFilteredSuggestions] = useState<string[]>([])

  useEffect(() => {
    if (value && suggestions.length > 0) {
      const filtered = suggestions.filter(s =>
        s.toLowerCase().includes(value.toLowerCase())
      ).slice(0, 8)
      setFilteredSuggestions(filtered)
      setShowSuggestions(filtered.length > 0 && value.length > 0)
    } else {
      setFilteredSuggestions([])
      setShowSuggestions(false)
    }
  }, [value, suggestions])

  return (
    <div className="relative flex-1">
      <Input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={() => value && setShowSuggestions(filteredSuggestions.length > 0)}
        onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
        placeholder={placeholder}
        className={className}
      />
      {showSuggestions && filteredSuggestions.length > 0 && (
        <div className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-md shadow-lg max-h-40 overflow-auto">
          {filteredSuggestions.map((suggestion) => (
            <button
              key={suggestion}
              type="button"
              className="w-full px-2 py-1.5 text-left text-xs hover:bg-gray-100 focus:bg-gray-100 focus:outline-none"
              onClick={() => {
                onChange(suggestion)
                setShowSuggestions(false)
              }}
            >
              {suggestion}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

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

interface OperationAutocompleteProps {
  value: string
  onChange: (value: string) => void
  onSelectOperation: (operation: ApiOperation) => void
  operations: ApiOperation[]
  placeholder?: string
  error?: string
}

function OperationAutocomplete({ value, onChange, onSelectOperation, operations, placeholder, error }: OperationAutocompleteProps) {
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [filteredOps, setFilteredOps] = useState<ApiOperation[]>([])

  useEffect(() => {
    if (value && operations.length > 0) {
      const filtered = operations.filter(op =>
        op.operationId.toLowerCase().includes(value.toLowerCase()) ||
        op.path.toLowerCase().includes(value.toLowerCase())
      ).slice(0, 10)
      setFilteredOps(filtered)
      setShowSuggestions(filtered.length > 0 && value.length > 0)
    } else {
      setFilteredOps([])
      setShowSuggestions(false)
    }
  }, [value, operations])

  return (
    <div className="relative">
      <Input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={() => value && setShowSuggestions(filteredOps.length > 0)}
        onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
        placeholder={placeholder}
        className={error ? 'border-destructive' : ''}
      />
      {showSuggestions && filteredOps.length > 0 && (
        <div className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-md shadow-lg max-h-60 overflow-auto">
          {filteredOps.map((op) => (
            <button
              key={op.id}
              type="button"
              className="w-full px-3 py-2 text-left hover:bg-gray-100 focus:bg-gray-100 focus:outline-none"
              onClick={() => {
                onSelectOperation(op)
                setShowSuggestions(false)
              }}
            >
              <div className="font-medium text-sm">{op.operationId}</div>
              <div className="text-xs text-muted-foreground">
                <span className="font-mono">{op.method}</span> {op.path}
              </div>
            </button>
          ))}
        </div>
      )}
      {error && <p className="text-sm text-destructive mt-1">{error}</p>}
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

            <div className="grid grid-cols-3 gap-3">
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
                <Label>Component (optional)</Label>
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
                <Label>Body (JSON)</Label>
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

export function OperationalSettingEditPage() {
  const { configId } = useParams<{ configId: string }>()
  const navigate = useNavigate()
  const { toast } = useToast()
  const { data: config, isLoading: isLoadingConfig } = useGetOperationalSetting(configId!)
  const updateOperationalSetting = useUpdateOperationalSetting()

  const [behaviorModelInputMode, setBehaviorModelInputMode] = useState<BehaviorModelInputMode>('manual')
  const [jsonValidationError, setJsonValidationError] = useState<string | null>(null)
  const [apiOperations, setApiOperations] = useState<ApiOperation[]>([])
  const [actors, setActors] = useState<string[]>([])
  const [newActor, setNewActor] = useState('')
  const [collapsedModels, setCollapsedModels] = useState<Set<number>>(new Set())

  const { refetch: refetchApiOperations } = useListApiOperations(
    { openApiFileId: config?.openApiFile?.id || '' },
    {
      query: {
        enabled: false,
      }
    }
  )

  const form = useForm<EditConfigFormData>({
    resolver: zodResolver(editConfigSchema),
    defaultValues: {
      name: '',
      description: '',
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

  // Load API operations when config is loaded
  useEffect(() => {
    const fetchApiOperations = async () => {
      if (!config?.openApiFile?.id) return

      try {
        const { data: ops } = await refetchApiOperations()
        if (ops) {
          setApiOperations(ops)
        }
      } catch (error) {
        console.error('Failed to fetch API operations:', error)
      }
    }

    fetchApiOperations()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [config?.openApiFile?.id])

  // Populate form with existing data
  useEffect(() => {
    if (!config) return

    // Transform operational profile from object to array
    const operationalProfileArray = Object.entries(config.operationalProfile || {}).map(([load, frequency]) => ({
      load: Number(load),
      frequency: Number(frequency),
    }))

    // Transform behavior models
    const behaviorModels = (config.usageProfile || []).map((model: any) => {
      // Convert steps to form format
      const steps = (model.steps || []).map((step: any) => ({
        method: step.method,
        path: step.path,
        operationId: step.operationId,
        component: step.component || '',
        waitMsFrom: step.waitMsFrom || 0,
        waitMsTo: step.waitMsTo || 0,
        headers: Object.entries(step.headers || {}).map(([key, value]) => ({ key, value: String(value) })),
        params: Object.entries(step.params || {}).map(([key, value]) => ({ key, value: String(value) })),
        body: JSON.stringify(step.body || {}, null, 2),
        save: Object.entries(step.save || {}).map(([key, value]) => ({ key, value: String(value) })),
      }))

      return {
        id: model.id,
        actor: model.actor,
        frequency: model.frequency,
        commonHeaders: [],
        steps,
      }
    })

    // Extract unique actors
    const uniqueActors = Array.from(new Set(behaviorModels.map((m: any) => m.actor)))
    setActors(uniqueActors)

    form.reset({
      name: config.name,
      description: config.description || '',
      behaviorModels,
      behaviorModelsJson: '',
      operationalProfile: operationalProfileArray,
    })
  }, [config, form])

  const keyValuePairsToObject = (pairs: { key: string; value: string }[]): Record<string, string> => {
    return pairs.reduce((acc, { key, value }) => {
      if (key.trim()) {
        acc[key] = value
      }
      return acc
    }, {} as Record<string, string>)
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

  const onSubmit = async (data: EditConfigFormData) => {
    if (!configId) return

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

      const operationalProfile = data.operationalProfile.reduce((acc: Record<string, number>, p: any) => {
        acc[p.load.toString()] = Number(p.frequency)
        return acc
      }, {})

      await updateOperationalSetting.mutateAsync({
        operationalSettingId: configId,
        data: {
          name: data.name,
          description: data.description || undefined,
          usageProfile: behaviorModels,
          operationalProfile,
        },
      })

      toast({
        title: 'Config updated',
        description: 'Your operational setting has been updated successfully',
      })

      navigate(`/operational-settings/${configId}`)
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to update config',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  if (isLoadingConfig) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (!config) {
    return (
      <div className="max-w-4xl mx-auto space-y-6">
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
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center gap-4">
        <Link to={`/operational-settings/${configId}`}>
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold">Edit Operational Setting</h1>
          <p className="text-muted-foreground">Update configuration for {config.name}</p>
        </div>
      </div>

      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        <Card>
          <CardContent className="space-y-6 pt-6">
            {/* Basic Info */}
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

              {/* Display OpenAPI file (read-only) */}
              <div className="space-y-2">
                <Label>OpenAPI File (cannot be changed)</Label>
                <Input value={config.openApiFile?.filename || 'N/A'} disabled />
              </div>
            </div>

            {/* Behavior Models */}
            <div className="space-y-4">
              <div className="space-y-3">
                <Label>Behavior Models Input Method</Label>
                <div className="grid grid-cols-2 gap-3">
                  <Button
                    type="button"
                    variant={behaviorModelInputMode === 'manual' ? 'default' : 'outline'}
                    className="w-full"
                    onClick={() => {
                      setBehaviorModelInputMode('manual')
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
                      setBehaviorModelInputMode('json')
                      const currentJson = form.watch('behaviorModelsJson')
                      if (currentJson) validateBehaviorModelsJson(currentJson)
                    }}
                  >
                    <FileJson className="h-4 w-4 mr-2" />
                    Raw JSON
                  </Button>
                </div>
              </div>

              {behaviorModelInputMode === 'json' && (
                <div className="space-y-2">
                  <Label htmlFor="behavior-models-json">Behavior Models JSON</Label>
                  <Textarea
                    id="behavior-models-json"
                    {...form.register('behaviorModelsJson', {
                      onChange: (e) => validateBehaviorModelsJson(e.target.value)
                    })}
                    placeholder={`[{"id": "checkout-flow", "actor": "Customer", "frequency": 0.7, "steps": [{"method": "GET", "path": "/api/products", "operationId": "listProducts", "headers": {}, "params": {}, "body": {}, "save": {}}]}]`}
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
                              <div className="flex items-end gap-2">
                                <div className="flex-1 grid grid-cols-2 gap-3">
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
                                </div>
                              </div>

                              <div className="space-y-2">
                                <Label>Behavior Model Frequency (0-1)</Label>
                                <Input type="number" step="0.01" min="0" max="1" {...form.register(`behaviorModels.${index}.frequency`)} />
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
          <Link to={`/operational-settings/${configId}`}>
            <Button type="button" variant="outline">Cancel</Button>
          </Link>
          <Button type="submit" disabled={updateOperationalSetting.isPending}>
            {updateOperationalSetting.isPending ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Updating...
              </>
            ) : (
              'Update Config'
            )}
          </Button>
        </div>
      </form>
    </div>
  )
}
