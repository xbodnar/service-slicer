/**
 * Benchmark hooks - wraps Orval-generated API client
 * This file wraps generated hooks to handle cache invalidation
 */

import {
  useListBenchmarks as useListBenchmarksGenerated,
  useGetBenchmark as useGetBenchmarkGenerated,
  useCreateBenchmark as useCreateBenchmarkGenerated,
  useUpdateBenchmark as useUpdateBenchmarkGenerated,
  useValidateOperationalSetting as useValidateOperationalSettingGenerated,
  getGetBenchmarkQueryKey,
} from '@/api/generated/benchmarks-controller/benchmarks-controller'
import { useQueryClient } from '@tanstack/react-query'

/**
 * Query hook for listing all benchmarks
 */
export function useBenchmarksList() {
  return useListBenchmarksGenerated()
}

/**
 * Query hook for getting a single benchmark
 */
export function useBenchmark(benchmarkId: string) {
  return useGetBenchmarkGenerated(benchmarkId, {
    query: {
      enabled: !!benchmarkId,
    },
  })
}

/**
 * Mutation hook for creating a benchmark
 * Wraps the generated hook to handle cache invalidation
 */
export function useCreateBenchmark() {
  const queryClient = useQueryClient()

  return useCreateBenchmarkGenerated({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['/benchmarks'] })
      },
    },
  })
}

/**
 * Mutation hook for updating a benchmark
 * Wraps the generated hook to handle cache invalidation
 */
export function useUpdateBenchmark() {
  const queryClient = useQueryClient()

  return useUpdateBenchmarkGenerated({
    mutation: {
      onSuccess: (_data, variables) => {
        // Invalidate benchmark list
        queryClient.invalidateQueries({ queryKey: ['/benchmarks'] })
        // Invalidate specific benchmark
        queryClient.invalidateQueries({
          queryKey: getGetBenchmarkQueryKey(variables.benchmarkId),
        })
      },
    },
  })
}

/**
 * Mutation hook for validating operational setting
 * Wraps the generated hook to handle cache invalidation
 */
export function useValidateOperationalSetting() {
  const queryClient = useQueryClient()

  return useValidateOperationalSettingGenerated({
    mutation: {
      onSuccess: (_data, variables) => {
        // Invalidate specific benchmark to refresh validation status
        queryClient.invalidateQueries({
          queryKey: getGetBenchmarkQueryKey(variables.benchmarkId),
        })
      },
    },
  })
}
