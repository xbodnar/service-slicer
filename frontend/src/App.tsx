import { Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from '@/components/ui/toaster'
import { MainLayout } from '@/components/layout/MainLayout'
import { ProjectListPage } from '@/pages/projects/ProjectListPage'
import { ProjectDetailPage } from '@/pages/projects/ProjectDetailPage'
import { ProjectCreatePage } from '@/pages/projects/ProjectCreatePage'
import { ExperimentListPage } from '@/pages/experiments/ExperimentListPage'
import { ExperimentDetailPage } from '@/pages/experiments/ExperimentDetailPage'
import { ExperimentCreatePage } from '@/pages/experiments/ExperimentCreatePage'

function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Navigate to="/projects" replace />} />

          {/* Projects */}
          <Route path="projects" element={<ProjectListPage />} />
          <Route path="projects/new" element={<ProjectCreatePage />} />
          <Route path="projects/:projectId" element={<ProjectDetailPage />} />

          {/* Experiments */}
          <Route path="experiments" element={<ExperimentListPage />} />
          <Route path="experiments/new" element={<ExperimentCreatePage />} />
          <Route path="experiments/:experimentId" element={<ExperimentDetailPage />} />
        </Route>
      </Routes>
      <Toaster />
    </>
  )
}

export default App
