/**
 * Project hooks - now using Orval-generated API client
 * This file re-exports generated hooks for backward compatibility
 *
 * Note: The backend doesn't properly export response DTOs for OpenAPI,
 * so we keep the manual types temporarily and cast the generated responses.
 */

import {
  useListProjects as useListProjectsGenerated,
  useGetProject as useGetProjectGenerated,
  useCreateProject as useCreateProjectGenerated,
  useRebuildGraph as useRebuildGraphGenerated,
} from '@/api/generated/project-controller/project-controller'
import { useQueryClient } from '@tanstack/react-query'
import type { CreateProjectRequest as GeneratedCreateProjectRequest } from '@/api/generated/openAPIDefinition.schemas'
import type {
  ListProjectsResponse,
  GetProjectResponse,
  CreateProjectRequest,
} from '@/types/api'

/**
 * Query hook for listing all projects
 */
export function useProjectsList() {
  const result = useListProjectsGenerated()
  return {
    ...result,
    data: result.data as unknown as ListProjectsResponse,
  }
}

/**
 * Query hook for getting a single project
 */
export function useProject(projectId: string) {
  const result = useGetProjectGenerated(projectId, {
    query: {
      enabled: !!projectId,
    },
  })
  return {
    ...result,
    data: result.data as unknown as GetProjectResponse,
  }
}

/**
 * Mutation hook for creating a project
 * Wraps the generated hook to handle cache invalidation
 */
export function useCreateProject() {
  const queryClient = useQueryClient()

  return useCreateProjectGenerated({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['/projects'] })
      },
    },
  })
}

/**
 * Mutation hook for rebuilding project graph
 * Wraps the generated hook to handle cache invalidation
 */
export function useRebuildProjectGraph() {
  const queryClient = useQueryClient()

  return useRebuildGraphGenerated({
    mutation: {
      onSuccess: (_, { projectId }) => {
        queryClient.invalidateQueries({ queryKey: ['/projects', projectId] })
      },
    },
  })
}

// Re-export types for convenience
export type { CreateProjectRequest, GeneratedCreateProjectRequest }
