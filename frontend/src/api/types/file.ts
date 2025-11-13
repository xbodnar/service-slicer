/**
 * Manual type definitions for file API responses
 * These should match the backend's ListFilesQuery.Result and FileSummary types
 */

export type FileStatus = 'PENDING' | 'READY' | 'FAILED'

export interface FileSummary {
  fileId: string
  filename: string
  expectedSize: number
  mimeType: string
  status: FileStatus
  createdAt: string
  updatedAt: string
}

export interface ListFilesResponse {
  files: FileSummary[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
}

export interface FileDownloadResponse {
  fileId: string
  filename: string
  downloadUrl: string
}
