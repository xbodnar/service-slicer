# Service Slicer

Service Slicer is a backend web application that helps developers decompose monolithic Java applications into microservices. It analyzes Java projects (uploaded as ZIP files or from GitHub repositories) and provides suggestions for microservice boundaries based on static code analysis.

## Features

- Upload Java projects as ZIP files or provide GitHub repository links
- Perform static code analysis to identify dependencies between classes and packages
- Generate dependency graphs to visualize relationships between components
- Suggest microservice boundaries based on code analysis
- Run analysis jobs in the background
- View detailed analysis results through a REST API

## Architecture

The application follows a modular architecture:

1. **Static Analyzer Module**
   - Parses Java code using JavaParser
   - Extracts class and package dependencies
   - Builds dependency graphs

2. **Dynamic Analyzer Module** (skeleton only, to be implemented later)
   - Interface for future implementation of runtime analysis

3. **Service Suggestion Engine**
   - Uses package structure and dependencies to suggest microservice boundaries
   - Calculates cohesion and coupling scores

4. **Data Aggregation & Correlation Layer**
   - Combines static analysis results
   - Provides comprehensive view of the application structure

## Technology Stack

- **Backend**: Kotlin with Spring Boot
- **Database**: PostgreSQL
- **Static Analysis**: JavaParser
- **Graph Processing**: Custom implementation
- **API**: RESTful endpoints

## Getting Started

### Prerequisites

- Java 21
- Maven
- PostgreSQL

### Setup

1. Clone the repository
2. Create a PostgreSQL database named `serviceslicer`
3. Update database credentials in `application.properties` if needed
4. Build the project:
   ```
   mvn clean install
   ```
5. Run the application:
   ```
   mvn spring-boot:run
   ```

## API Endpoints

### Projects

- `GET /api/projects` - Get all projects
- `GET /api/projects/{id}` - Get project by ID
- `POST /api/projects/upload` - Upload a new project as ZIP file
- `POST /api/projects/github` - Create a new project from GitHub repository
- `DELETE /api/projects/{id}` - Delete a project

### Analysis Jobs

- `GET /api/analysis-jobs/project/{projectId}` - Get all analysis jobs for a project
- `GET /api/analysis-jobs/{id}` - Get analysis job by ID
- `POST /api/analysis-jobs` - Create a new analysis job
- `GET /api/analysis-jobs/{id}/result` - Get analysis result for a job
- `GET /api/analysis-jobs/{id}/result/detailed` - Get detailed analysis result for a job

## Example Usage

### Upload a Java project

```bash
curl -X POST -F "file=@project.zip" -F "project={\"name\":\"My Project\",\"description\":\"My project description\"}" http://localhost:8080/api/projects/upload
```

### Create a project from GitHub

```bash
curl -X POST -H "Content-Type: application/json" -d '{"name":"GitHub Project","description":"Project from GitHub","repositoryUrl":"https://github.com/username/repo","branch":"main"}' http://localhost:8080/api/projects/github
```

### Create an analysis job

```bash
curl -X POST -H "Content-Type: application/json" -d '{"projectId":1,"analysisType":"STATIC"}' http://localhost:8080/api/analysis-jobs
```

### Get analysis results

```bash
curl -X GET http://localhost:8080/api/analysis-jobs/1/result
```

## Future Enhancements

- Implement dynamic code analysis
- Add more sophisticated graph clustering algorithms
- Provide visualization of dependency graphs
- Support for other languages beyond Java
- Integration with CI/CD pipelines