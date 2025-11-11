import { useState } from 'react'
import { uploadFile, extractZipFile } from '@/api/files'
import { useToast } from '@/components/ui/use-toast'

export interface UploadedFile {
  fileId: string
  filename: string
  size: number
}

export interface UseFileUploadResult {
  uploadFile: (file: File) => Promise<UploadedFile | null>
  extractZip: (fileId: string) => Promise<string | null>
  isUploading: boolean
  progress: number
}

/**
 * Hook for handling file uploads with the 3-step flow
 */
export function useFileUpload(): UseFileUploadResult {
  const [isUploading, setIsUploading] = useState(false)
  const [progress, setProgress] = useState(0)
  const { toast } = useToast()

  const handleUploadFile = async (file: File): Promise<UploadedFile | null> => {
    setIsUploading(true)
    setProgress(0)

    try {
      setProgress(25)
      const fileId = await uploadFile(file)
      setProgress(100)

      toast({
        title: 'Upload successful',
        description: `${file.name} has been uploaded.`,
      })

      return {
        fileId,
        filename: file.name,
        size: file.size,
      }
    } catch (error) {
      console.error('File upload failed:', error)
      toast({
        variant: 'destructive',
        title: 'Upload failed',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
      return null
    } finally {
      setIsUploading(false)
      setProgress(0)
    }
  }

  const handleExtractZip = async (fileId: string): Promise<string | null> => {
    try {
      const { dirId } = await extractZipFile(fileId)
      toast({
        title: 'Extraction successful',
        description: 'ZIP file has been extracted.',
      })
      return dirId
    } catch (error) {
      console.error('ZIP extraction failed:', error)
      toast({
        variant: 'destructive',
        title: 'Extraction failed',
        description: error instanceof Error ? error.message : 'An unknown error occurred',
      })
      return null
    }
  }

  return {
    uploadFile: handleUploadFile,
    extractZip: handleExtractZip,
    isUploading,
    progress,
  }
}
