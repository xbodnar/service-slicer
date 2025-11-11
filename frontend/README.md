# ServiceSlicer Frontend

React + TypeScript frontend application for ServiceSlicer, a tool for analyzing Java monolithic applications and suggesting microservice boundaries.

## Features

### Project Management
- **Create Projects**: Upload JAR files and optional source code (ZIP) for analysis
- **View Projects**: Browse all analysis projects
- **Project Details**: View dependency graphs and decomposition results from 5 different algorithms:
  - Label Propagation
  - Louvain
  - Leiden
  - Domain-Driven Decomposition
  - Actor-Driven Decomposition

### Load Test Experiments
- **Create Experiments**: Set up performance comparison experiments
- **Configure Systems**: Add multiple systems under test (e.g., monolith vs microservices)
- **Upload Artifacts**: Upload docker-compose files, JAR files, and OpenAPI specifications
- **View Results**: Compare performance metrics across different decomposition strategies

## Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **React Router v6** - Client-side routing
- **TanStack Query** - Server state management
- **React Hook Form** - Form handling
- **Zod** - Schema validation
- **Tailwind CSS** - Styling
- **Shadcn/ui** - UI components
- **Axios** - HTTP client

## Getting Started

### Prerequisites

- Node.js 18+ and npm
- Backend API running on `http://localhost:8080`

### Installation

```bash
# Install dependencies
npm install
```

### Configuration

Create a `.env` file (or use the provided `.env.example`):

```env
VITE_API_BASE_URL=/api
```

### Development

```bash
# Start development server on http://localhost:3000
npm run dev
```

The dev server includes a proxy configuration that forwards `/api/*` requests to `http://localhost:8080`.

### Build

```bash
# Build for production
npm run build

# Preview production build
npm run preview
```

### Linting

```bash
npm run lint
```

## Project Structure

```
src/
├── api/              # API client and HTTP calls
│   ├── client.ts     # Axios instance
│   ├── files.ts      # File upload/management endpoints
│   ├── projects.ts   # Project CRUD endpoints
│   └── experiments.ts # Experiment CRUD endpoints
├── components/
│   ├── ui/           # Reusable UI components (Shadcn)
│   └── layout/       # Layout components (header, nav, etc.)
├── hooks/            # Custom React hooks
│   ├── useFileUpload.ts   # File upload with 3-step flow
│   ├── useProjects.ts     # React Query hooks for projects
│   └── useExperiments.ts  # React Query hooks for experiments
├── lib/              # Utility functions
│   └── utils.ts      # Class merging (cn), etc.
├── pages/            # Page components
│   ├── projects/
│   │   ├── ProjectListPage.tsx
│   │   ├── ProjectDetailPage.tsx
│   │   └── ProjectCreatePage.tsx
│   └── experiments/
│       ├── ExperimentListPage.tsx
│       ├── ExperimentDetailPage.tsx
│       └── ExperimentCreatePage.tsx
├── styles/           # Global styles
│   └── index.css     # Tailwind imports and theme
├── types/            # TypeScript type definitions
│   └── api.ts        # API request/response types
├── App.tsx           # Root component with routes
└── main.tsx          # Application entry point
```

## API Integration

The frontend communicates with the Spring Boot backend via REST API:

### File Upload Flow
1. `POST /files` - Initiate upload, get presigned URL
2. `PUT <uploadUrl>` - Upload file to storage
3. `POST /files/{fileId}/complete` - Mark upload complete
4. `POST /files/{fileId}/extract` - Extract ZIP files (optional)

### Projects
- `GET /projects` - List all projects
- `POST /projects` - Create new project
- `GET /projects/{id}` - Get project details
- `POST /projects/{id}/graph` - Rebuild dependency graph

### Experiments
- `GET /load-tests/experiments` - List all experiments
- `POST /load-tests/experiments` - Create new experiment
- `GET /load-tests/experiments/{id}` - Get experiment details

## Key Features

### Responsive File Upload
The `useFileUpload` hook handles the complete 3-step file upload process with progress tracking and error handling.

### Form Validation
All forms use React Hook Form with Zod schema validation for type-safe, validated inputs.

### Optimistic UI Updates
React Query automatically manages cache invalidation and refetching after mutations.

### Error Handling
Toast notifications provide user feedback for all operations (success, error, loading states).

## Development Notes

- The proxy configuration in `vite.config.ts` forwards API requests to the backend
- TypeScript types in `src/types/api.ts` match the backend Kotlin DTOs
- All UI components follow the Shadcn/ui design system
- Loading states and error boundaries are implemented on all pages
