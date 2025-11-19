import axios, { AxiosRequestConfig, AxiosResponse } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// Create axios instance
const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Response interceptor for error handling
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    // Extract detail from error responses (prioritize "detail" field for 400 errors)
    if (error.response?.data?.detail) {
      // Create a new error with the detail message
      const enhancedError = new Error(error.response.data.detail)
      // Preserve the original error properties
      Object.assign(enhancedError, {
        response: error.response,
        request: error.request,
        config: error.config,
        code: error.code,
      })
      console.error(`API Error (${error.response.status}):`, error.response.data.detail)
      return Promise.reject(enhancedError)
    }

    // For other errors, log and pass through
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// Orval mutator function - used by generated code
export const apiClient = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  return axiosInstance({
    ...config,
    ...options,
  }).then(({ data }: AxiosResponse<T>) => data)
}

// Export the axios instance for manual API calls (backward compatibility)
export { axiosInstance }
