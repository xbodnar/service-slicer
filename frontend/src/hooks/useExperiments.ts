/**
 * Experiment hooks - wraps Orval-generated API client
 * This file wraps generated hooks to handle cache invalidation
 */

import {
  useListExperiments as useListExperimentsGenerated,
  useGetExperiment as useGetExperimentGenerated,
  useCreateExperiment as useCreateExperimentGenerated,
} from '@/api/generated/load-test-experiments-controller/load-test-experiments-controller'
import { useQueryClient } from '@tanstack/react-query'

/**
 * Query hook for listing all experiments
 */
export function useExperimentsList() {
  return useListExperimentsGenerated()
}

/**
 * Query hook for getting a single experiment
 */
export function useExperiment(experimentId: string) {
  return useGetExperimentGenerated(experimentId, {
    query: {
      enabled: !!experimentId,
    },
  })
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
