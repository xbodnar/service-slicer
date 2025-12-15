# Service Slicer

A tool for analyzing monolithic applications and comparing performance across different architectural strategies.

## Features

### Monolith Decomposition
- Upload Java JAR files for static code analysis
- Parses bytecode to extract class dependencies and builds a graph representation
- Runs community detection algorithms (Louvain, Leiden, Label Propagation) to suggest microservice boundaries
- AI-assisted refinement for cluster naming and merge/split recommendations

### Benchmarking & Load Testing
- Define systems under test via Docker Compose configurations
- Supports any application stack (not limited to Java)
- Integrates with k6 for performance testing
- Compare metrics across different deployment strategies

## Prerequisites

- Java 21+
- Node.js 18+
- Docker & Docker Compose

## Quick Start

### Backend

```bash
cd backend

# Set OpenAI API key (required for AI-assisted features)
export OPENAI_API_KEY=your-api-key-here

# Build and run (Docker services start automatically)
./mvnw spring-boot:run
```

The backend will be available at http://localhost:8080

- Swagger UI: http://localhost:8080/swagger-ui.html
- PostgreSQL: localhost:33771
- Neo4j Browser: http://localhost:7474

### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Generate API client (backend must be running)
npm run generate:api

# Start development server
npm run dev
```

The frontend will be available at http://localhost:3000

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Kotlin, Spring Boot, Spring Batch |
| Frontend | React, TypeScript, Vite, TanStack Query |
| Databases | PostgreSQL, Neo4j |
| Storage | MinIO (S3-compatible) |
| Load Testing | k6 |
| AI | OpenAI API |