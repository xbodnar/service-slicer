import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createProject,
  listProjects,
  getProject,
  rebuildProjectGraph,
} from '@/api/projects'
import type { CreateProjectRequest } from '@/types/api'

/**
 * Query hook for listing all projects
 */
export function useProjectsList() {
  return useQuery({
    queryKey: ['projects'],
    queryFn: listProjects,
  })
}

/**
 * Query hook for getting a single project
 */
export function useProject(projectId: string) {
  return useQuery({
    queryKey: ['projects', projectId],
    queryFn: () => getProject(projectId),
    enabled: !!projectId,
  })
}

/**
 * Mutation hook for creating a project
 */
export function useCreateProject() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: CreateProjectRequest) => createProject(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
    },
  })
}

/**
 * Mutation hook for rebuilding project graph
 */
export function useRebuildProjectGraph() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (projectId: string) => rebuildProjectGraph(projectId),
    onSuccess: (_, projectId) => {
      queryClient.invalidateQueries({ queryKey: ['projects', projectId] })
    },
  })
}
