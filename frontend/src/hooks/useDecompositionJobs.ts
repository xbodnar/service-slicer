import {
    useCreateDecompositionJob as useCreateDecompositionJobGenerated,
    useGetDecompositionJobSummary,
    useListDecompositionJobs,
} from "@/api/generated/decomposition-job-controller/decomposition-job-controller.ts";
import {useQueryClient} from "@tanstack/react-query";
import type {ListDecompositionJobsParams} from "@/api/generated/openAPIDefinition.schemas.ts";

/**
 * Query hook for listing all decomposition jobs
 */
export function useDecompositionJobsList(params?: ListDecompositionJobsParams) {
    const result = useListDecompositionJobs(params)
    return {
        ...result,
        data: result.data
    }
}

/**
 * Query hook for getting a single decomposition job
 */
export function useDecompositionJob(decompositionJobId: string, options?: { refetchInterval?: number | ((data: any) => number | false) }) {
    const result = useGetDecompositionJobSummary(decompositionJobId, {
        query: {
            enabled: !!decompositionJobId,
            refetchInterval: options?.refetchInterval,
            refetchIntervalInBackground: true, // Continue polling even when tab is not active
        },
    })
    return {
        ...result,
        data: result.data,
    }
}

/**
 * Mutation hook for creating a decomposition job
 * Wraps the generated hook to handle cache invalidation
 */
export function useCreateDecompositionJob() {
    const queryClient = useQueryClient()

    return useCreateDecompositionJobGenerated({
        mutation: {
            onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: ['/decomposition-jobs'] })
            },
        },
    })
}