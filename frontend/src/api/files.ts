import { apiClient } from './client'
import type {
  InitiateFileUploadRequest,
  InitiateFileUploadResponse,
  ExtractZipFileResponse,
  FetchGitRepositoryRequest,
  FetchGitRepositoryResponse,
} from '@/types/api'
import axios from 'axios'

/**
 * Initiate a file upload and get a presigned URL
 */
export const initiateFileUpload = async (
  request: InitiateFileUploadRequest
): Promise<InitiateFileUploadResponse> => {
  const { data } = await apiClient.post<InitiateFileUploadResponse>('/files', request)
  return data
}

/**
 * Upload file to the presigned URL
 */
export const uploadFileToUrl = async (
  uploadUrl: string,
  file: File
): Promise<void> => {
  await axios.put(uploadUrl, file, {
    headers: {
      'Content-Type': file.type,
    },
  })
}

/**
 * Mark the file upload as complete
 */
export const completeFileUpload = async (fileId: string): Promise<void> => {
  await apiClient.post(`/files/${fileId}/complete`)
}

/**
 * Extract a ZIP file
 */
export const extractZipFile = async (fileId: string): Promise<ExtractZipFileResponse> => {
  const { data } = await apiClient.post<ExtractZipFileResponse>(`/files/${fileId}/extract`)
  return data
}

/**
 * Fetch a git repository
 */
export const fetchGitRepository = async (
  request: FetchGitRepositoryRequest
): Promise<FetchGitRepositoryResponse> => {
  const { data } = await apiClient.post<FetchGitRepositoryResponse>('/files/git', request)
  return data
}

/**
 * Complete 3-step file upload flow
 */
export const uploadFile = async (file: File): Promise<string> => {
  // Step 1: Initiate upload
  const { fileId, uploadUrl } = await initiateFileUpload({
    filename: file.name,
    size: file.size,
    mimeType: file.type,
    contentHash: 'placeholder', // In production, you'd compute the hash
  })

  // Step 2: Upload to presigned URL
  await uploadFileToUrl(uploadUrl, file)

  // Step 3: Complete upload
  await completeFileUpload(fileId)

  return fileId
}
