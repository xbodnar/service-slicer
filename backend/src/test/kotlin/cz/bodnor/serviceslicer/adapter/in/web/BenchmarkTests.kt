package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.IntegrationTest
import cz.bodnor.serviceslicer.adapter.out.minio.MinioConnector
import cz.bodnor.serviceslicer.domain.file.File
import cz.bodnor.serviceslicer.domain.file.FileRepository
import cz.bodnor.serviceslicer.toUUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.nio.file.Path

@Disabled
@AutoConfigureMockMvc
class BenchmarkTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val fileRepository: FileRepository,
    @Autowired private val minioConnector: MinioConnector,
) : IntegrationTest() {

    @BeforeEach
    fun setupFiles() {
        // Create OpenAPI file
        val openApiFile = File(
            id = 1.toUUID(),
            filename = "openapi.json",
            fileSize = 1024,
            mimeType = "application/json",
        )
        openApiFile.markAsReady()
        minioConnector.uploadFile(Path.of("src/test/resources/openapi.json"), "${1.toUUID()}/openapi.json")
        fileRepository.save(openApiFile)

        // Create JAR file
        val jarFile = File(
            id = 2.toUUID(),
            filename = "blog-app.jar",
            fileSize = 2048,
            mimeType = "application/zip",
        )
        jarFile.markAsReady()
        fileRepository.save(jarFile)

        // Create compose file
        val composeFile = File(
            id = 3.toUUID(),
            filename = "docker-compose.yml",
            fileSize = 3072,
            mimeType = "application/yaml",
        )
        composeFile.markAsReady()
        fileRepository.save(composeFile)
    }

    @Test
    fun `should create benchmark with valid request`() {
        // given

        val requestBody = """
        {
          "name": "Baseline Performance Test",
          "description": "Testing baseline performance with monolithic architecture",
          "loadTestConfig": {
            "openApiFileId": "00000000-0000-0000-0000-000000000001",
            "behaviorModels": [
              {
                "id": "bm1",
                "actor": "Customer",
                "usageProfile": 0.7,
                "steps": [
                  {
                    "method": "GET",
                    "path": "/api/products",
                    "headers": {},
                    "params": {},
                    "body": {},
                    "save": {}
                  },
                  {
                    "method": "POST",
                    "path": "/api/orders",
                    "headers": {"Content-Type": "application/json"},
                    "params": {},
                    "body": {"productId": 1, "quantity": 2},
                    "save": {}
                  }
                ],
                "thinkFrom": 1000,
                "thinkTo": 3000
              },
              {
                "id": "bm2",
                "actor": "Admin",
                "usageProfile": 0.3,
                "steps": [
                  {
                    "method": "GET",
                    "path": "/api/admin/users",
                    "headers": {},
                    "params": {},
                    "body": {},
                    "save": {}
                  }
                ],
                "thinkFrom": 2000,
                "thinkTo": 5000
              }
            ],
            "operationalProfile": [
              {
                "load": 10,
                "frequency": 0.5
              },
              {
                "load": 50,
                "frequency": 0.3
              },
              {
                "load": 100,
                "frequency": 0.2
              }
            ],
            "generateBehaviorModels": false
          },
          "systemsUnderTest": [
            {
              "name": "Monolithic Architecture",
              "jarFileId": "00000000-0000-0000-0000-000000000002",
              "composeFileId": "00000000-0000-0000-0000-000000000003",
              "description": "Original monolithic application",
              "healthCheckPath": "/actuator/health",
              "appPort": 9090,
              "startupTimeoutSeconds": 180
            }
          ]
        }
        """.trimIndent()

        // when & then
        mockMvc.post("/benchmarks") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }.andExpect {
            status { isOk() }
            jsonPath("$.benchmarkId") { exists() }
        }
    }

    @Test
    fun `should return 400 when file does not exist`() {
        // given
        val requestBody = """
        {
          "name": "Test",
          "loadTestConfig": {
            "openApiFileId": "00000000-000-0000-0000-000000000001",
            "behaviorModels": [
              {
                "id": "bm1",
                "actor": "User",
                "usageProfile": 1.0,
                "steps": [
                  {
                    "method": "GET",
                    "path": "/api/test",
                    "headers": {},
                    "params": {},
                    "body": {},
                    "save": {}
                  }
                ],
                "thinkFrom": 1000,
                "thinkTo": 2000
              }
            ],
            "operationalProfile": [
              {
                "load": 10,
                "frequency": 1.0
              }
            ],
            "generateBehaviorModels": false
          },
          "systemsUnderTest": [
            {
              "name": "Test SUT",
              "jarFileId": "00000000-0000-0000-0000-000000000002",
              "composeFileId": "00000000-0000-0000-0000-000000000004",
              "description": "Test SUT",
              "healthCheckPath": "/actuator/health",
              "appPort": 9090,
              "startupTimeoutSeconds": 180
            }
          ]
        }
        """.trimIndent()

        // when & then
        mockMvc.post("/benchmarks") {
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }.andExpect {
            status { is4xxClientError() }
        }
    }
}
