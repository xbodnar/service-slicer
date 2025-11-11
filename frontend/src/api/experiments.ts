import { apiClient } from './client'
import type {
  CreateLoadTestExperimentRequest,
  CreateLoadTestExperimentResponse,
  ListLoadTestExperimentsResponse,
  GetLoadTestExperimentResponse,
} from '@/types/api'

/**
 * Create a new load test experiment
 */
export const createExperiment = async (
  request: CreateLoadTestExperimentRequest
): Promise<CreateLoadTestExperimentResponse> => {
  const { data } = await apiClient.post<CreateLoadTestExperimentResponse>(
    '/load-tests/experiments',
    request
  )
  return data
}

/**
 * List all experiments
 */
export const listExperiments = async (): Promise<ListLoadTestExperimentsResponse> => {
  const { data } = await apiClient.get<ListLoadTestExperimentsResponse>(
    '/load-tests/experiments'
  )
  return data
}

/**
 * Get a single experiment by ID
 */
export const getExperiment = async (
  experimentId: string
): Promise<GetLoadTestExperimentResponse> => {
  const { data } = await apiClient.get<GetLoadTestExperimentResponse>(
    `/load-tests/experiments/${experimentId}`
  )
  return data
}
