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
import { useToast } from '@/components/ui/use-toast'
import { Download } from 'lucide-react'
import {Pagination} from "@/components/ui/pagination.tsx";
import {useState} from "react";

export function FileListPage() {
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const { data, isLoading } = useListFiles({ page, size })
  const { toast } = useToast()

  const handleDownload = async (fileId: string, filename: string) => {
    try {
      const response = await fetch(`/api/files/${fileId}/download`)
      if (!response.ok) {
        throw new Error('Failed to get download URL')
      }
      const data = await response.json()

      // Fetch as blob to force download (works cross-origin)
      const fileResponse = await fetch(data.downloadUrl)
      if (!fileResponse.ok) {
        throw new Error('File not found or unavailable')
      }
      const blob = await fileResponse.blob()

      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()

      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      toast({
        variant: 'destructive',
        title: 'Download failed',
        description: error instanceof Error ? error.message : 'Failed to download file'
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

  if (isLoading) {
    return <div className="p-8">Loading files...</div>
  }

  const files = data?.items || []

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
              files.map((file) => (
                <TableRow key={file.id}>
                  <TableCell className="font-medium">{file.filename}</TableCell>
                  <TableCell>{formatFileSize(file.fileSize)}</TableCell>
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
                      onClick={() => handleDownload(file.id, file.filename)}
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

      {data && data.totalPages > 1 && (
        <Pagination
          currentPage={data.currentPage}
          totalPages={data.totalPages}
          pageSize={data.pageSize}
          totalElements={data.totalElements}
          onPageChange={setPage}
        />
      )}
    </div>
  )
}
