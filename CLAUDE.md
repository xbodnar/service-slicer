# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Service Slicer

A tool for analyzing Java monolithic applications and suggesting microservice boundaries through static code analysis and performance testing. The application accepts Java projects via ZIP upload or GitHub repository links, analyzes dependencies, generates dependency graphs, and compares performance metrics across different decomposition strategies.

## Repository Structure

```
ServiceSlicer/
├── backend/          # Kotlin + Spring Boot REST API
└── frontend/         # React + TypeScript SPA
```

## Backend (Kotlin + Spring Boot)

### Build & Development Commands

**All commands should be run from the `backend/` directory.**

```bash
# Build the project
mvn clean install

# Run the application (auto-starts PostgreSQL & Neo4j via Docker Compose)
mvn spring-boot:run

# Run all tests
mvn test

# Run a specific test
mvn test -Dtest=BuildDependencyGraphTest

# Check code formatting
mvn ktlint:check

# Auto-format Kotlin code
mvn ktlint:format
```

### Database Setup

The project uses Docker Compose for local development. Running `mvn spring-boot:run` automatically starts required containers via Spring Boot's Docker Compose support.

**Services (from compose.yaml):**
- **PostgreSQL**: localhost:33771 (user: postgres, password: postgres, db: serviceslicer)
- **Neo4j**: localhost:7474 (HTTP), localhost:7687 (Bolt) (user: neo4j, password: password)

**Database migrations** are managed by Liquibase (changelog: `src/main/resources/db/changelog/db.changelog.xml`).

### API Documentation

The backend exposes OpenAPI 3.0 documentation:
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Swagger UI**: http://localhost:8080/swagger-ui.html

The frontend auto-generates TypeScript client code from this spec using Orval.

### Architecture

#### Hexagonal Architecture (Ports & Adapters)

The backend follows hexagonal architecture organized into layers:

**1. Domain Layer** (`domain/`)
- Core business entities: `Project`, `AnalysisJob`, `ClassNode`, `File`, `LoadTestExperiment`, `SystemUnderTest`, `LoadTestConfig`
- Domain services handle entity creation and business logic
- No dependencies on infrastructure or application layers

**2. Application Layer** (`application/`)
- Organized into modules by business capability (`project`, `file`, `loadtestexperiment`, `analysis`)
- Command handlers implement business use cases
- Uses CQRS pattern with `CommandBus` and `QueryBus`

**3. Adapter Layer** (`adapter/`)
- **Inbound**: REST controllers (`web/`), Spring Batch tasklets (`batch/`), event listeners (`event/`)
- **Outbound**: Database repositories, external API clients (MinIO, GitHub, OpenAI)

**4. Infrastructure Layer** (`infrastructure/`)
- CQRS infrastructure (`cqrs/`): CommandBus, QueryBus, handlers
- Spring configuration (`config/`)
- Spring Batch job configuration (`job/`)

#### CQRS Pattern

Commands and queries are separated:

**Commands** (mutating operations):
```kotlin
// Execute via CommandBus
commandBus(CreateProjectCommand(name = "My Project", jarFileId = fileId))
```

**Queries** (read operations):
```kotlin
// Execute via QueryBus
queryBus(GetProjectQuery(projectId = id))
```

Command/Query handlers are in `application/module/*/` directories.

### Code Conventions

- Kotlin source: `src/main/kotlin`, tests: `src/test/kotlin`
- Code must pass `ktlint:check` (validated in CI)
- JPA entities use Kotlin `data class` with JPA/no-arg plugins
- Domain entities extend `DomainEntity` (or `UpdatableEntity` for timestamp tracking)
- Use `invoke()` operator for functional-style command/query bus calls

### Key Backend Features

**Static Analysis**
- Parses Java code with JavaParser
- Extracts class dependencies into Neo4j graph database
- Runs community detection algorithms (Louvain, Leiden, Label Propagation)
- Uses JGraphT for graph analysis

**Load Test Experiments**
- Manages multiple Systems Under Test (SUT) configurations
- Integrates with k6 for performance testing
- Compares performance metrics across different architectures

**File Management**
- 3-step upload flow: create → presigned URL → complete
- Stores artifacts in MinIO (S3-compatible storage)
- Supports ZIP extraction for source code

**AI Integration**
- OpenAI integration for decomposition refinement
- Cluster naming and merge/split suggestions

## Frontend (React + TypeScript)

### Build & Development Commands

**All commands should be run from the `frontend/` directory.**

```bash
# Install dependencies
npm install

# Generate TypeScript API client from backend OpenAPI spec
# (Backend must be running on http://localhost:8080)
npm run generate:api

# Start development server (http://localhost:3000)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

### Development Workflow

**Important**: Always generate API client before starting development:

```bash
# Terminal 1: Start backend
cd backend && mvn spring-boot:run

# Terminal 2: Generate API client, then start frontend
cd frontend
npm run generate:api  # Run this after backend API changes
npm run dev
```

### Tech Stack

- **React 18** + **TypeScript** - UI library with type safety
- **Vite** - Build tool and dev server with proxy to backend
- **React Router v6** - Client-side routing
- **TanStack Query** - Server state management and caching
- **React Hook Form** + **Zod** - Type-safe form handling and validation
- **Tailwind CSS** + **Shadcn/ui** - Styling and UI components
- **Axios** - HTTP client (configured in `src/api/client.ts`)
- **Orval** - Auto-generates TypeScript types and React Query hooks from OpenAPI

### API Client Code Generation

The frontend uses **Orval** to auto-generate type-safe API client code:

- **Input**: OpenAPI spec from `http://localhost:8080/v3/api-docs`
- **Output**: `src/api/generated/` (React Query hooks + TypeScript types)
- **Regenerate**: Run `npm run generate:api` after backend API changes
- **Config**: `orval.config.ts` (uses `src/api/client.ts` as HTTP mutator)

Generated code includes:
- TypeScript types matching backend DTOs (`src/api/generated/model/`)
- React Query hooks for all endpoints (`src/api/generated/projects/`, etc.)
- Automatic type safety between frontend and backend

### Project Structure

```
src/
├── api/
│   ├── generated/        # 🤖 Auto-generated by Orval (do not edit)
│   │   ├── model/        # TypeScript types from OpenAPI
│   │   ├── projects/     # Project API hooks
│   │   ├── experiments/  # Experiment API hooks
│   │   └── files/        # File API hooks
│   ├── client.ts         # Axios instance (base URL, interceptors)
│   └── files.ts          # Custom 3-step file upload logic
├── components/
│   ├── ui/               # Reusable UI components (Shadcn)
│   ├── layout/           # Layout components (MainLayout, header, etc.)
│   └── graph/            # Graph visualization (Neo4j NVL)
├── hooks/                # Custom React hooks
│   └── useFileUpload.ts  # 3-step file upload with progress
├── pages/                # Page components
│   ├── projects/         # Project CRUD pages
│   └── experiments/      # Experiment CRUD pages
├── lib/                  # Utility functions (cn, etc.)
├── App.tsx               # Root component with routes
└── main.tsx              # Application entry point
```

### Key Frontend Features

**Auto-Generated API Client**
- Type-safe API calls prevent runtime errors
- Automatic sync between backend and frontend types
- React Query hooks with built-in caching and optimistic updates

**3-Step File Upload**
- `POST /files` → Initiate upload, get presigned URL
- `PUT <uploadUrl>` → Upload file to MinIO storage
- `POST /files/{fileId}/complete` → Mark upload complete
- Handled by `useFileUpload` hook with progress tracking

**Graph Visualization**
- Uses Neo4j NVL React component for dependency graph visualization
- Interactive graph exploration with zoom, pan, and node selection

**Form Validation**
- React Hook Form + Zod schema validation for all forms
- Type-safe, validated inputs with error handling

## Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Backend** | Kotlin + Spring Boot | REST API, job orchestration |
| **Frontend** | React + TypeScript + Vite | SPA with type safety |
| **Batch Processing** | Spring Batch | Async job processing |
| **Static Analysis** | JavaParser | AST parsing and dependency extraction |
| **Graph Store** | Neo4j + JGraphT | Code graph, clustering algorithms |
| **Metrics/Tracing** | Micrometer + Prometheus | Performance monitoring |
| **Load Testing** | k6 | Performance benchmarking |
| **Database** | PostgreSQL + Liquibase | Job metadata, results, migrations |
| **Object Storage** | MinIO (S3-compatible) | Artifact storage (ZIPs, JARs) |
| **AI Integration** | OpenAI API | Decomposition refinement |
| **API Contract** | OpenAPI 3.0 + Orval | Type-safe client generation |

## Development Workflow

### Full Stack Development

```bash
# 1. Start backend (auto-starts Docker services)
cd backend
mvn spring-boot:run

# 2. Generate frontend API client (new terminal)
cd frontend
npm run generate:api

# 3. Start frontend dev server
npm run dev

# 4. Access application
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Making API Changes

When modifying backend API endpoints:

1. Update backend code and restart server
2. Verify changes in Swagger UI
3. Regenerate frontend client: `cd frontend && npm run generate:api`
4. Update frontend code to use new types/hooks

### CI/CD

GitHub Actions workflows (`.github/workflows/`):
- **ci.yml**: Runs ktlint check, compilation, and tests on PRs
- **claude.yml**: Claude Code integration for PR reviews

## Important Reminders

- **Backend**: Run `mvn ktlint:format` before committing to pass CI checks
- **Frontend**: Run `npm run generate:api` after backend API changes
- **Database**: Migrations are auto-applied on startup via Liquibase
- **Docker**: Services start automatically with `mvn spring-boot:run`
- **API Docs**: Always available at http://localhost:8080/swagger-ui.html