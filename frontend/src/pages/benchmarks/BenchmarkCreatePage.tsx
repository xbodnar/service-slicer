import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { useToast } from '@/components/ui/use-toast'
import { useCreateBenchmark } from '@/hooks/useBenchmarks'
import { useListSystemsUnderTest } from '@/api/generated/system-under-test-controller/system-under-test-controller'
import { useListOperationalSettings } from '@/api/generated/operational-setting-controller/operational-setting-controller'
import { ArrowLeft, Loader2, Server, Settings } from 'lucide-react'
import { useState } from 'react'

const benchmarkSchema = z
  .object({
    name: z.string().min(1, 'Benchmark name is required'),
    description: z.string().optional(),
    operationalSettingId: z.string().min(1, 'Operational setting is required'),
    systemsUnderTest: z.array(z.string()).min(1, 'At least one system under test is required'),
    baselineSutId: z.string().min(1, 'Baseline system is required'),
  })
  .refine((data) => data.systemsUnderTest.includes(data.baselineSutId), {
    message: 'Baseline system must be one of the selected systems under test',
    path: ['baselineSutId'],
  })

type BenchmarkFormData = z.infer<typeof benchmarkSchema>

export function BenchmarkCreatePage() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const createBenchmark = useCreateBenchmark()
  const { data: systemsUnderTestList, isLoading: sutsLoading } = useListSystemsUnderTest()
  const { data: operationalSettingsList, isLoading: configsLoading } = useListOperationalSettings()

  const [selectedSystems, setSelectedSystems] = useState<string[]>([])
  const [baselineSutId, setBaselineSutId] = useState<string>('')

  const form = useForm<BenchmarkFormData>({
    resolver: zodResolver(benchmarkSchema),
    defaultValues: {
      name: '',
      description: '',
      operationalSettingId: '',
      systemsUnderTest: [],
      baselineSutId: '',
    },
  })

  const systems = systemsUnderTestList?.items || []
  const operationalSettings = operationalSettingsList?.items || []

  const handleSystemToggle = (systemId: string, checked: boolean) => {
    const newSelectedSystems = checked
      ? [...selectedSystems, systemId]
      : selectedSystems.filter((id) => id !== systemId)

    setSelectedSystems(newSelectedSystems)
    form.setValue('systemsUnderTest', newSelectedSystems)

    // Clear baseline if it's no longer in selected systems
    if (!checked && baselineSutId === systemId) {
      setBaselineSutId('')
      form.setValue('baselineSutId', '')
    }
  }

  const handleBaselineChange = (systemId: string) => {
    setBaselineSutId(systemId)
    form.setValue('baselineSutId', systemId)
  }

  const onSubmit = async (data: BenchmarkFormData) => {
    const selectedSetting = operationalSettings.find((setting: any) => setting.id === data.operationalSettingId)

    if (!selectedSetting) {
      toast({
        variant: 'destructive',
        title: 'Operational setting required',
        description: 'Please select an operational setting before creating the benchmark.',
      })
      return
    }

    try {
      const result = await createBenchmark.mutateAsync({
        data: {
          name: data.name,
          description: data.description || undefined,
          operationalSettingId: selectedSetting.id,
          systemsUnderTest: data.systemsUnderTest,
          baselineSutId: data.baselineSutId,
        },
      })

      toast({
        title: 'Benchmark created',
        description: 'Your benchmark has been created successfully',
      })

      navigate(`/benchmarks/${result.id}`)
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Failed to create benchmark',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
    }
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/benchmarks">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-4 w-4" />
          </Button>
        </Link>
        <div>
          <h1 className="text-3xl font-bold">Create Benchmark</h1>
          <p className="text-muted-foreground">Select multiple systems under test and an operational setting</p>
        </div>
      </div>

      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>Benchmark Details</CardTitle>
            <CardDescription>Provide the basics to create the benchmark.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Name *</Label>
              <Input id="name" {...form.register('name')} placeholder="Performance Comparison Test" />
              {form.formState.errors.name && (
                <p className="text-sm text-destructive">{String(form.formState.errors.name.message)}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description (optional)</Label>
              <Textarea
                id="description"
                {...form.register('description')}
                placeholder="Compare performance between monolithic and microservices architecture"
                rows={3}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Operational Setting</CardTitle>
            <CardDescription>Select an existing operational setting to reuse.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Label htmlFor="operational-setting">Operational Setting *</Label>
            {configsLoading ? (
              <div className="flex items-center justify-center py-4">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
              </div>
            ) : operationalSettings.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                No operational settings found. Create one first, then return to make a benchmark.
              </p>
            ) : (
              <Select
                value={form.watch('operationalSettingId')}
                onValueChange={(value) => form.setValue('operationalSettingId', value)}
              >
                <SelectTrigger id="operational-setting">
                  <SelectValue placeholder="Select an operational setting" />
                </SelectTrigger>
                <SelectContent>
                  {operationalSettings.map((setting: any) => (
                    <SelectItem key={setting.id} value={setting.id}>
                      <div className="flex items-center gap-2">
                        <Settings className="h-4 w-4" />
                        <span>{setting.name}</span>
                        {setting.description && (
                          <span className="text-xs text-muted-foreground">- {setting.description}</span>
                        )}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
            {form.formState.errors.operationalSettingId && (
              <p className="text-sm text-destructive">
                {String(form.formState.errors.operationalSettingId.message)}
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Systems Under Test</CardTitle>
            <CardDescription>
              Select one or more systems to test, then mark exactly one as the baseline.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {sutsLoading ? (
              <div className="flex items-center justify-center py-4">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
              </div>
            ) : systems.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                No systems available. Create Systems Under Test first, then return to make a benchmark.
              </p>
            ) : (
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label>Select Systems *</Label>
                  <div className="space-y-2 border rounded-md p-4">
                    {systems.map((system: any) => (
                      <div key={system.id} className="flex items-center space-x-3">
                        <Checkbox
                          id={`system-${system.id}`}
                          checked={selectedSystems.includes(system.id)}
                          onCheckedChange={(checked) => handleSystemToggle(system.id, checked as boolean)}
                        />
                        <Label
                          htmlFor={`system-${system.id}`}
                          className="flex items-center gap-2 cursor-pointer font-normal"
                        >
                          <Server className="h-4 w-4" />
                          <span>{system.name}</span>
                          {system.description && (
                            <span className="text-xs text-muted-foreground">- {system.description}</span>
                          )}
                        </Label>
                      </div>
                    ))}
                  </div>
                  {form.formState.errors.systemsUnderTest && (
                    <p className="text-sm text-destructive">
                      {String(form.formState.errors.systemsUnderTest.message)}
                    </p>
                  )}
                </div>

                {selectedSystems.length > 0 && (
                  <div className="space-y-2">
                    <Label>Baseline System *</Label>
                    <p className="text-sm text-muted-foreground">
                      Select which system will serve as the performance baseline.
                    </p>
                    <RadioGroup value={baselineSutId} onValueChange={handleBaselineChange}>
                      <div className="space-y-2 border rounded-md p-4">
                        {systems
                          .filter((system: any) => selectedSystems.includes(system.id))
                          .map((system: any) => (
                            <div key={system.id} className="flex items-center space-x-3">
                              <RadioGroupItem value={system.id} id={`baseline-${system.id}`} />
                              <Label
                                htmlFor={`baseline-${system.id}`}
                                className="flex items-center gap-2 cursor-pointer font-normal"
                              >
                                <Server className="h-4 w-4" />
                                <span>{system.name}</span>
                              </Label>
                            </div>
                          ))}
                      </div>
                    </RadioGroup>
                    {form.formState.errors.baselineSutId && (
                      <p className="text-sm text-destructive">
                        {String(form.formState.errors.baselineSutId.message)}
                      </p>
                    )}
                  </div>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        <div className="flex justify-end gap-4">
          <Link to="/benchmarks">
            <Button type="button" variant="outline">
              Cancel
            </Button>
          </Link>
          <Button type="submit" disabled={createBenchmark.isPending || configsLoading || sutsLoading}>
            {createBenchmark.isPending ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Creating...
              </>
            ) : (
              'Create Benchmark'
            )}
          </Button>
        </div>
      </form>
    </div>
  )
}
