import { Routes, Route } from 'react-router-dom'
import { Toaster } from '@/components/ui/toaster'
import { AuthProvider } from '@/contexts/AuthContext'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { MainLayout } from '@/components/layout/MainLayout'
import { LandingPage } from '@/pages/LandingPage'
import { LoginPage } from '@/pages/auth/LoginPage'
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
import {DecompositionJobListPage} from "@/pages/decompositionjobs/DecompositionJobListPage.tsx";
import {DecompositionJobCreatePage} from "@/pages/decompositionjobs/DecompositionJobCreatePage.tsx";
import {DecompositionJobDetailPage} from "@/pages/decompositionjobs/DecompositionJobDetailPage.tsx";

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<MainLayout />}>
          <Route index element={<LandingPage />} />

          {/* Decomposition Jobs */}
          <Route path="decomposition-jobs" element={<DecompositionJobListPage />} />
          <Route path="decomposition-jobs/new" element={<ProtectedRoute><DecompositionJobCreatePage /></ProtectedRoute>} />
          <Route path="decomposition-jobs/:decompositionJobId" element={<DecompositionJobDetailPage />} />

          {/* Benchmarks */}
          <Route path="benchmarks" element={<BenchmarkListPage />} />
          <Route path="benchmarks/new" element={<ProtectedRoute><BenchmarkCreatePage /></ProtectedRoute>} />
          <Route path="benchmarks/:benchmarkId" element={<BenchmarkDetailPage />} />
          <Route path="benchmarks/:benchmarkId/runs" element={<BenchmarkRunListPage />} />
          <Route path="benchmarks/:benchmarkId/runs/:runId" element={<BenchmarkRunDetailPage />} />

          {/* Operational Settings */}
          <Route path="operational-settings" element={<OperationalSettingListPage />} />
          <Route path="operational-settings/new" element={<ProtectedRoute><OperationalSettingCreatePage /></ProtectedRoute>} />
          <Route path="operational-settings/:configId" element={<OperationalSettingDetailPage />} />

          {/* Systems Under Test */}
          <Route path="systems-under-test" element={<SystemUnderTestListPage />} />
          <Route path="systems-under-test/new" element={<ProtectedRoute><SystemUnderTestCreatePage /></ProtectedRoute>} />
          <Route path="systems-under-test/:sutId" element={<SystemUnderTestDetailPage />} />

          {/* Files */}
          <Route path="files" element={<FileListPage />} />
        </Route>
      </Routes>
      <Toaster />
    </AuthProvider>
  )
}

export default App
