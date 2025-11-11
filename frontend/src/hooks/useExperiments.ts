import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createExperiment,
  listExperiments,
  getExperiment,
} from '@/api/experiments'
import type { CreateLoadTestExperimentRequest } from '@/types/api'

/**
 * Query hook for listing all experiments
 */
export function useExperimentsList() {
  return useQuery({
    queryKey: ['experiments'],
    queryFn: listExperiments,
  })
}

/**
 * Query hook for getting a single experiment
 */
export function useExperiment(experimentId: string) {
  return useQuery({
    queryKey: ['experiments', experimentId],
    queryFn: () => getExperiment(experimentId),
    enabled: !!experimentId,
  })
}

/**
 * Mutation hook for creating an experiment
 */
export function useCreateExperiment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: CreateLoadTestExperimentRequest) =>
      createExperiment(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['experiments'] })
    },
  })
}
