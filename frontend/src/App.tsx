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
import { BenchmarkRunListPage } from '@/pages/benchmarks/BenchmarkRunListPage'
import { BenchmarkRunDetailPage } from '@/pages/benchmarks/BenchmarkRunDetailPage'
import { OperationalSettingListPage } from '@/pages/operationalsettings/OperationalSettingListPage'
import { OperationalSettingDetailPage } from '@/pages/operationalsettings/OperationalSettingDetailPage'
import { OperationalSettingCreatePage } from '@/pages/operationalsettings/OperationalSettingCreatePage'
import { FileListPage } from '@/pages/files/FileListPage'
import { SystemUnderTestListPage } from '@/pages/sut/SystemUnderTestListPage'
import { SystemUnderTestDetailPage } from '@/pages/sut/SystemUnderTestDetailPage'
import { SystemUnderTestCreatePage } from '@/pages/sut/SystemUnderTestCreatePage'

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
          <Route path="benchmarks/:benchmarkId/runs" element={<BenchmarkRunListPage />} />
          <Route path="benchmarks/:benchmarkId/runs/:runId" element={<BenchmarkRunDetailPage />} />

          {/* Operational Settings */}
          <Route path="operational-settings" element={<OperationalSettingListPage />} />
          <Route path="operational-settings/new" element={<OperationalSettingCreatePage />} />
          <Route path="operational-settings/:configId" element={<OperationalSettingDetailPage />} />

          {/* Systems Under Test */}
          <Route path="systems-under-test" element={<SystemUnderTestListPage />} />
          <Route path="systems-under-test/new" element={<SystemUnderTestCreatePage />} />
          <Route path="systems-under-test/:sutId" element={<SystemUnderTestDetailPage />} />

          {/* Files */}
          <Route path="files" element={<FileListPage />} />
        </Route>
      </Routes>
      <Toaster />
    </>
  )
}

export default App
