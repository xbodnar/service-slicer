import { Routes, Route } from 'react-router-dom'
import { Toaster } from '@/components/ui/toaster'
import { MainLayout } from '@/components/layout/MainLayout'
import { LandingPage } from '@/pages/LandingPage'
import { ProjectListPage } from '@/pages/projects/ProjectListPage'
import { ProjectDetailPage } from '@/pages/projects/ProjectDetailPage'
import { ProjectCreatePage } from '@/pages/projects/ProjectCreatePage'
import { BenchmarkListPage } from '@/pages/benchmarks/BenchmarkListPage'
import { BenchmarkDetailPage } from '@/pages/benchmarks/BenchmarkDetailPage'
import { BenchmarkCreatePage } from '@/pages/benchmarks/BenchmarkCreatePage'
import { FileListPage } from '@/pages/files/FileListPage'

function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<LandingPage />} />

          {/* Projects */}
          <Route path="projects" element={<ProjectListPage />} />
          <Route path="projects/new" element={<ProjectCreatePage />} />
          <Route path="projects/:projectId" element={<ProjectDetailPage />} />

          {/* Benchmarks */}
          <Route path="benchmarks" element={<BenchmarkListPage />} />
          <Route path="benchmarks/new" element={<BenchmarkCreatePage />} />
          <Route path="benchmarks/:benchmarkId" element={<BenchmarkDetailPage />} />

          {/* Files */}
          <Route path="files" element={<FileListPage />} />
        </Route>
      </Routes>
      <Toaster />
    </>
  )
}

export default App
