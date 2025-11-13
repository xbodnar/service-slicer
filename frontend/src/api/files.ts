/**
 * File API - now using Orval-generated API client
 * This file wraps generated functions for backward compatibility
 *
 * Note: The backend doesn't properly export response DTOs for OpenAPI,
 * so we keep the manual types and cast the generated responses.
 */

import {
  initiateUpload,
  completeUpload,
  extractZipFile as extractZipFileGenerated,
  fetchGitRepository as fetchGitRepositoryGenerated,
} from '@/api/generated/file-controller/file-controller'
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
  const result = await initiateUpload(request)
  return result as unknown as InitiateFileUploadResponse
}

/**
 * Upload file to the presigned URL
 * This doesn't go through our backend, so we use axios directly
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
  await completeUpload(fileId)
}

/**
 * Extract a ZIP file
 */
export const extractZipFile = async (fileId: string): Promise<ExtractZipFileResponse> => {
  const result = await extractZipFileGenerated(fileId)
  return result as unknown as ExtractZipFileResponse
}

/**
 * Fetch a git repository
 */
export const fetchGitRepository = async (
  request: FetchGitRepositoryRequest
): Promise<FetchGitRepositoryResponse> => {
  const result = await fetchGitRepositoryGenerated(request)
  return result as unknown as FetchGitRepositoryResponse
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
