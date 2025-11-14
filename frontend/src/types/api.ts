// ============================================================================
// File API Types
// ============================================================================

export interface InitiateFileUploadRequest {
  filename: string
  size: number
  mimeType: string
}

export interface InitiateFileUploadResponse {
  fileId: string
  uploadUrl: string
  storageKey: string
}

export interface ExtractZipFileResponse {
  dirId: string
}

export interface FetchGitRepositoryRequest {
  repositoryUrl: string
  branch: string
}

export interface FetchGitRepositoryResponse {
  dirId: string
}

// ============================================================================
// Project API Types
// ============================================================================

export interface CreateProjectRequest {
  projectName: string
  basePackageName: string
  excludePackages: string[]
  jarFileId: string
  projectDirId: string | null
}

export interface CreateProjectResponse {
  projectId: string
}

export interface ProjectSummary {
  projectId: string
  name: string
  basePackageName: string
  createdAt: string
}

export interface ListProjectsResponse {
  projects: ProjectSummary[]
}

export interface GetProjectResponse {
  projectId: string
  name: string
  analysisJobResult: AnalysisJobResult
}

export interface AnalysisJobResult {
  staticAnalysis: StaticAnalysisResult
}

export interface StaticAnalysisResult {
  dependencyGraph: GraphSummary
  labelPropagationAlgorithm: DecompositionResults
  louvainAlgorithm: DecompositionResults
  leidenAlgorithm: DecompositionResults
  domainDrivenDecomposition: DecompositionResults
  actorDrivenDecomposition: DecompositionResults
}

export interface GraphSummary {
  nodeCount: number
  edgeCount: number
  nodes: ClassNodeDto[]
}

export interface ClassNodeDto {
  simpleClassName: string
  fullyQualifiedClassName: string
  dependencies: string[] // List of FQNs that this class depends on
}

export interface DecompositionResults {
  communities: Record<string, string[]> // communityId -> class FQNs
}

// ============================================================================
// Load Test Experiment API Types
// ============================================================================

export interface FileDto {
  fileId: string
  filename: string
  fileSize: number
}

export interface ApiRequest {
  method: string
  path: string
  headers: Record<string, string>
  params: Record<string, string>
  body?: string
}

export interface BehaviorModel {
  id: string
  actor: string
  usageProfile: number
  steps: ApiRequest[]
  thinkFrom: number
  thinkTo: number
}

export interface OperationalProfile {
  loadsToFreq: Array<{ first: number; second: number }>
}

export interface CreateLoadTestConfigDto {
  openApiFileId: string
  behaviorModels?: BehaviorModel[]
  operationalProfile?: OperationalProfile | null
}

export interface CreateSystemUnderTestDto {
  name: string
  composeFileId: string
  jarFileId: string
  description?: string | null
  healthCheckPath?: string
  appPort?: number
  startupTimeoutSeconds?: number
}

export interface CreateLoadTestExperimentRequest {
  name: string
  description?: string | null
  loadTestConfig: CreateLoadTestConfigDto
  systemsUnderTest: CreateSystemUnderTestDto[]
}

export interface CreateLoadTestExperimentResponse {
  experimentId: string
}

export interface ExperimentSummary {
  experimentId: string
  name: string
  description: string | null
  createdAt: string
}

export interface ListLoadTestExperimentsResponse {
  experiments: ExperimentSummary[]
}

export interface LoadTestConfigDto {
  loadTestConfigId: string
  openApiFile: FileDto
  behaviorModels: BehaviorModel[]
  operationalProfile: OperationalProfile | null
}

export interface SystemUnderTestDto {
  systemUnderTestId: string
  name: string
  composeFile: FileDto
  jarFile: FileDto
  description: string | null
  healthCheckPath: string
  appPort: number
  startupTimeoutSeconds: number
}

export interface GetLoadTestExperimentResponse {
  experimentId: string
  name: string
  description: string | null
  loadTestConfig: LoadTestConfigDto
  systemsUnderTest: SystemUnderTestDto[]
  createdAt: string
  updatedAt: string
}
