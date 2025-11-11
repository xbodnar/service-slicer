/**
 * Experiment hooks - now using Orval-generated API client
 * This file re-exports generated hooks for backward compatibility
 *
 * Note: The backend doesn't properly export response DTOs for OpenAPI,
 * so we keep the manual types temporarily and cast the generated responses.
 */

import {
  useListExperiments as useListExperimentsGenerated,
  useGetExperiment as useGetExperimentGenerated,
  useCreateExperiment as useCreateExperimentGenerated,
} from '@/api/generated/load-test-experiments-controller/load-test-experiments-controller'
import { useQueryClient } from '@tanstack/react-query'
import type {
  ListLoadTestExperimentsResponse,
  GetLoadTestExperimentResponse,
  CreateLoadTestExperimentRequest,
} from '@/types/api'

/**
 * Query hook for listing all experiments
 */
export function useExperimentsList() {
  const result = useListExperimentsGenerated()
  return {
    ...result,
    data: result.data as unknown as ListLoadTestExperimentsResponse,
  }
}

/**
 * Query hook for getting a single experiment
 */
export function useExperiment(experimentId: string) {
  const result = useGetExperimentGenerated(experimentId, {
    query: {
      enabled: !!experimentId,
    },
  })
  return {
    ...result,
    data: result.data as unknown as GetLoadTestExperimentResponse,
  }
}

/**
 * Mutation hook for creating an experiment
 * Wraps the generated hook to handle cache invalidation
 */
export function useCreateExperiment() {
  const queryClient = useQueryClient()

  return useCreateExperimentGenerated({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['/load-tests/experiments'] })
      },
    },
  })
}

// Re-export type for convenience
export type { CreateLoadTestExperimentRequest }
