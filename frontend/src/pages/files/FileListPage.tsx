import { useListFiles } from '@/api/generated/file-controller/file-controller'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Download } from 'lucide-react'

export function FileListPage() {
  const { data, isLoading } = useListFiles({ page: 0, size: 100 })

  const handleDownload = async (fileId: string, filename: string) => {
    try {
      const response = await fetch(`/api/files/${fileId}/download`)
      const data = await response.json()

      // Fetch the file as a blob to force download
      const fileResponse = await fetch(data.downloadUrl)
      const blob = await fileResponse.blob()

      // Create a temporary download link
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()

      // Cleanup
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      console.error('Failed to download file:', error)
    }
  }

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  if (isLoading) {
    return <div className="p-8">Loading files...</div>
  }

  const files = (data as any)?.files || []

  return (
    <div className="container mx-auto py-8">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">Files</h1>
        <p className="text-muted-foreground">
          Manage uploaded files
        </p>
      </div>

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Filename</TableHead>
              <TableHead>Size</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Created</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {files.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  No files uploaded yet
                </TableCell>
              </TableRow>
            ) : (
              files.map((file: any) => (
                <TableRow key={file.fileId}>
                  <TableCell className="font-medium">{file.filename}</TableCell>
                  <TableCell>{formatFileSize(file.expectedSize)}</TableCell>
                  <TableCell className="text-muted-foreground">{file.mimeType}</TableCell>
                  <TableCell>
                    <span className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${
                      file.status === 'READY'
                        ? 'bg-green-100 text-green-800'
                        : file.status === 'PENDING'
                        ? 'bg-yellow-100 text-yellow-800'
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {file.status}
                    </span>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {new Date(file.createdAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleDownload(file.fileId, file.filename)}
                      disabled={file.status !== 'READY'}
                    >
                      <Download className="h-4 w-4" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
