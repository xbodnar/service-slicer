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
import { useToast } from '@/components/ui/use-toast'
import { useCreateBenchmark } from '@/hooks/useBenchmarks'
import { useListSystemsUnderTest } from '@/api/generated/system-under-test-controller/system-under-test-controller'
import { useListOperationalSettings } from '@/api/generated/operational-setting-controller/operational-setting-controller'
import { ArrowLeft, Loader2, Server, Settings } from 'lucide-react'

const benchmarkSchema = z
  .object({
    name: z.string().min(1, 'Benchmark name is required'),
    description: z.string().optional(),
    operationalSettingId: z.string().min(1, 'Operational setting is required'),
    baselineSutId: z.string().min(1, 'Baseline system is required'),
    targetSutId: z.string().min(1, 'Target system is required'),
  })
  .refine((data) => data.baselineSutId !== data.targetSutId, {
    message: 'Baseline and target systems must be different',
    path: ['targetSutId'],
  })

type BenchmarkFormData = z.infer<typeof benchmarkSchema>

export function BenchmarkCreatePage() {
  const navigate = useNavigate()
  const { toast } = useToast()
  const createBenchmark = useCreateBenchmark()
  const { data: systemsUnderTestList, isLoading: sutsLoading } = useListSystemsUnderTest()
  const { data: operationalSettingsList, isLoading: configsLoading } = useListOperationalSettings()

  const form = useForm<BenchmarkFormData>({
    resolver: zodResolver(benchmarkSchema),
    defaultValues: {
      name: '',
      description: '',
      operationalSettingId: '',
      baselineSutId: '',
      targetSutId: '',
    },
  })

  const systems = systemsUnderTestList?.items || []
  const operationalSettings = operationalSettingsList?.items || []

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
          baselineSutId: data.baselineSutId,
          targetSutId: data.targetSutId,
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
          <p className="text-muted-foreground">Reference an existing SUT pair and operational setting</p>
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
            <CardDescription>Pick the baseline and target systems to compare.</CardDescription>
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
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="baseline-sut">Baseline System *</Label>
                  <Select
                    value={form.watch('baselineSutId')}
                    onValueChange={(value) => form.setValue('baselineSutId', value)}
                  >
                    <SelectTrigger id="baseline-sut">
                      <SelectValue placeholder="Select baseline system" />
                    </SelectTrigger>
                    <SelectContent>
                      {systems.map((system: any) => (
                        <SelectItem key={system.id} value={system.id}>
                          <div className="flex items-center gap-2">
                            <Server className="h-4 w-4" />
                            {system.name}
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {form.formState.errors.baselineSutId && (
                    <p className="text-sm text-destructive">
                      {String(form.formState.errors.baselineSutId.message)}
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="target-sut">Target System *</Label>
                  <Select
                    value={form.watch('targetSutId')}
                    onValueChange={(value) => form.setValue('targetSutId', value)}
                  >
                    <SelectTrigger id="target-sut">
                      <SelectValue placeholder="Select target system" />
                    </SelectTrigger>
                    <SelectContent>
                      {systems.map((system: any) => (
                        <SelectItem key={system.id} value={system.id}>
                          <div className="flex items-center gap-2">
                            <Server className="h-4 w-4" />
                            {system.name}
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {form.formState.errors.targetSutId && (
                    <p className="text-sm text-destructive">
                      {String(form.formState.errors.targetSutId.message)}
                    </p>
                  )}
                </div>
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
