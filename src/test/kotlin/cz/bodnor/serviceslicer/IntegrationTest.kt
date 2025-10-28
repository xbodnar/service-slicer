package cz.bodnor.serviceslicer

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.Container
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.utility.MountableFile

@SpringBootTest
@ActiveProfiles("test")
abstract class IntegrationTest {

    @Autowired
    internal lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    internal lateinit var helperService: TestHelperService

    @BeforeEach
    fun setup() {
        jdbcTemplate.execute(TRUNCATE_ALL_TABLES_SQL)
    }

    companion object {
        private val neo4jContainer = Neo4jContainer("neo4j:5.18.0")
            .withPlugins("apoc", "graph-data-science")
            .withEnv("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,gds.*")
            .withEnv("NEO4J_dbms_security_procedures_allowlist", "apoc.*,gds.*")
            .withEnv("NEO4J_apoc_import_file_enabled", "true")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("all.cypher"),
                "/var/lib/neo4j/import/all.cypher",
            )

        init {
            neo4jContainer.start()

            val result: Container.ExecResult = neo4jContainer.execInContainer(
                "cypher-shell",
                "-u", "neo4j",
                "-p", neo4jContainer.adminPassword,
                "--database", "neo4j",
                "-f", "/var/lib/neo4j/import/all.cypher",
            )

            check(result.exitCode == 0) { "Failed to load all.cypher: ${result.stderr}" }
        }

        @DynamicPropertySource
        @JvmStatic
        fun neo4jProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.neo4j.uri") { neo4jContainer.boltUrl }
            registry.add("spring.neo4j.authentication.username") { "neo4j" }
            registry.add("spring.neo4j.authentication.password") { neo4jContainer.adminPassword }
        }
    }
}

private const val TRUNCATE_ALL_TABLES_SQL = """
DO
${'$'}do${'$'}
	BEGIN
		EXECUTE
			(SELECT 'TRUNCATE TABLE ' || string_agg(oid::regclass::text, ', ') || ' CASCADE'
			 FROM   pg_class
			 WHERE  relkind = 'r'  -- only tables
			   AND    relnamespace = 'public'::regnamespace
			   AND relname NOT IN ('databasechangelog', 'databasechangeloglock')
			);
	END
${'$'}do${'$'};
"""
