import { useState } from 'react'
import { Label } from '@/components/ui/label'
import { FileInput } from '@/components/ui/file-input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Badge } from '@/components/ui/badge'
import { useListFiles } from '@/api/generated/file-controller/file-controller'
import type { ListFilesResponse } from '@/api/types/file'
import { useFileUpload, type UploadedFile } from '@/hooks/useFileUpload'
import { Package, CheckCircle2, Loader2 } from 'lucide-react'

interface FileSelectorProps {
  id: string
  label: string
  accept?: string
  required?: boolean
  onFileSelected: (file: UploadedFile | null) => void
  selectedFile?: UploadedFile | null
  mimeTypeFilter?: string
}

export function FileSelector({
  id,
  label,
  accept,
  required = false,
  onFileSelected,
  selectedFile,
  mimeTypeFilter,
}: FileSelectorProps) {
  const [mode, setMode] = useState<'upload' | 'existing'>('upload')
  const { uploadFile, isUploading } = useFileUpload()
  const { data: filesData } = useListFiles({ page: 0, size: 100 })

  // Filter files by READY status and optionally by MIME type
  const availableFiles = ((filesData as ListFilesResponse)?.files || []).filter(
    (file) => {
      if (file.status !== 'READY') return false
      if (mimeTypeFilter) {
        const mimeType = file.mimeType.toLowerCase()
        const filter = mimeTypeFilter.toLowerCase()

        // Special handling for common file types
        if (filter === 'jar') {
          return mimeType.includes('java-archive') || mimeType.includes('jar')
        } else if (filter === 'yaml') {
          return mimeType.includes('yaml') || mimeType.includes('yml')
        } else if (filter === 'json') {
          return mimeType.includes('json') && file.filename.toLowerCase().endsWith('.json')
        } else if (filter === 'zip') {
          return mimeType.includes('zip')
        } else {
          return mimeType.includes(filter)
        }
      }
      return true
    }
  )

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    const result = await uploadFile(file)
    if (result) {
      onFileSelected(result)
    }
  }

  const handleExistingFileSelect = (fileId: string) => {
    const selectedExistingFile = availableFiles.find((f) => f.fileId === fileId)
    if (selectedExistingFile) {
      onFileSelected({
        fileId: selectedExistingFile.fileId,
        filename: selectedExistingFile.filename,
        size: selectedExistingFile.expectedSize,
      })
    }
  }

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  return (
    <div className="space-y-3">
      <Label htmlFor={id}>
        {label} {required && <span className="text-destructive">*</span>}
      </Label>

      <RadioGroup value={mode} onValueChange={(value) => setMode(value as 'upload' | 'existing')}>
        <div className="flex items-center space-x-2">
          <RadioGroupItem value="upload" id={`${id}-upload`} />
          <Label htmlFor={`${id}-upload`} className="font-normal cursor-pointer">
            Upload new file
          </Label>
        </div>
        <div className="flex items-center space-x-2">
          <RadioGroupItem value="existing" id={`${id}-existing`} />
          <Label htmlFor={`${id}-existing`} className="font-normal cursor-pointer">
            Select existing file
          </Label>
        </div>
      </RadioGroup>

      {mode === 'upload' ? (
        <div className="space-y-2">
          <FileInput
            id={id}
            accept={accept}
            onChange={handleFileUpload}
            disabled={isUploading}
          />
          {isUploading && (
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              Uploading...
            </div>
          )}
        </div>
      ) : (
        <Select
          onValueChange={handleExistingFileSelect}
          value={selectedFile?.fileId}
        >
          <SelectTrigger>
            <SelectValue placeholder="Select a file..." />
          </SelectTrigger>
          <SelectContent>
            {availableFiles.length === 0 ? (
              <div className="p-2 text-sm text-muted-foreground">No files available</div>
            ) : (
              availableFiles.map((file) => (
                <SelectItem key={file.fileId} value={file.fileId}>
                  <div className="flex items-center gap-2">
                    <span className="truncate">{file.filename}</span>
                    <span className="text-xs text-muted-foreground">
                      ({formatFileSize(file.expectedSize)})
                    </span>
                  </div>
                </SelectItem>
              ))
            )}
          </SelectContent>
        </Select>
      )}

      {selectedFile && (
        <div className="flex items-center gap-2 p-3 rounded-lg bg-muted/50 border">
          <Package className="h-4 w-4 text-primary flex-shrink-0" />
          <span className="text-sm font-medium flex-1 truncate">{selectedFile.filename}</span>
          <Badge variant="secondary" className="text-xs">
            {formatFileSize(selectedFile.size)}
          </Badge>
          <CheckCircle2 className="h-4 w-4 text-green-500 flex-shrink-0" />
        </div>
      )}
    </div>
  )
}
