import { apiClient } from './client'
import type {
  CreateProjectRequest,
  CreateProjectResponse,
  ListProjectsResponse,
  GetProjectResponse,
} from '@/types/api'

/**
 * Create a new project
 */
export const createProject = async (
  request: CreateProjectRequest
): Promise<CreateProjectResponse> => {
  const { data } = await apiClient.post<CreateProjectResponse>('/projects', request)
  return data
}

/**
 * List all projects
 */
export const listProjects = async (): Promise<ListProjectsResponse> => {
  const { data } = await apiClient.get<ListProjectsResponse>('/projects')
  return data
}

/**
 * Get a single project by ID
 */
export const getProject = async (projectId: string): Promise<GetProjectResponse> => {
  const { data } = await apiClient.get<GetProjectResponse>(`/projects/${projectId}`)
  return data
}

/**
 * Rebuild dependency graph for a project
 */
export const rebuildProjectGraph = async (projectId: string): Promise<void> => {
  await apiClient.post(`/projects/${projectId}/graph`)
}
