package cz.bodnor.serviceslicer.infrastructure.config

import org.neo4j.cypherdsl.core.renderer.Dialect
import org.neo4j.driver.Driver
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.neo4j.core.DatabaseSelectionProvider
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.data.neo4j.core.Neo4jOperations
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.neo4j.cypherdsl.core.renderer.Configuration as Neo4jConfiguration

@Configuration
@EnableTransactionManagement
class Neo4jConfig {

    @Bean
    fun cypherDslConfiguration(): Neo4jConfiguration =
        Neo4jConfiguration.newConfig().withDialect(Dialect.NEO4J_5).build()

    // Workaround for missing neo4jTransactionManager, check https://github.com/spring-projects/spring-data-neo4j/issues/2931
    @Bean("neo4jTemplate")
    @ConditionalOnMissingBean(Neo4jOperations::class)
    fun neo4jTemplate(
        neo4jClient: Neo4jClient,
        neo4jMappingContext: Neo4jMappingContext,
        driver: Driver,
        databaseNameProvider: DatabaseSelectionProvider,
        optionalCustomizers: ObjectProvider<TransactionManagerCustomizers?>,
    ): Neo4jTemplate {
        val transactionManager = Neo4jTransactionManager(driver, databaseNameProvider)
        optionalCustomizers.ifAvailable { customizer ->
            customizer?.customize(transactionManager)
        }
        return Neo4jTemplate(neo4jClient, neo4jMappingContext, transactionManager)
    }
}
