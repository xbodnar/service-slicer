package cz.bodnor.serviceslicer.adapter.out.ai

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.analysis.port.out.ApplyDomainExpertDecomposition
import cz.bodnor.serviceslicer.application.module.decomposition.command.DomainExpertDecompositionCommand.DomainDecompositionType
import cz.bodnor.serviceslicer.domain.graph.ClassNode
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.stereotype.Component

@Component
class ApplyDomainExpertDecompositionAi(
    chatClientBuilder: ChatClient.Builder,
    private val objectMapper: ObjectMapper,
) : ApplyDomainExpertDecomposition {

    private val chatClient: ChatClient = chatClientBuilder.build()

    private val outputConverter = BeanOutputConverter(ApplyDomainExpertDecomposition.Result::class.java)

    private val jsonSchema = outputConverter.jsonSchema

    override fun invoke(
        graph: List<ClassNode>,
        type: DomainDecompositionType,
    ): ApplyDomainExpertDecomposition.Result {
        val rawPrompt = when (type) {
            DomainDecompositionType.DOMAIN_DRIVEN -> DDD_SYSTEM_PROMPT
            DomainDecompositionType.ACTOR_DRIVEN -> ADD_SYSTEM_PROMPT
        }

        val promptInput = graph.map { node ->
            ClassGraphData(
                className = node.simpleClassName,
                fullyQualifiedName = node.fullyQualifiedClassName,
                type = node.type.name,
                dependencies = node.dependencies.map { it.target.fullyQualifiedClassName },
            )
        }

        val prompt = Prompt(
            rawPrompt.replace("{{graphInput}}", objectMapper.writeValueAsString(promptInput)),
            OpenAiChatOptions.builder()
                .model(OpenAiApi.ChatModel.GPT_4_1_MINI)
                .responseFormat(ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, jsonSchema))
                .build(),
        )

        val response = chatClient.prompt(prompt).call()
        val content = response.content() ?: throw IllegalStateException("No AI response: ${response.content()}")

        return outputConverter.convert(content) ?: error("Failed to parse AI response for $type decomposition")
    }

    data class ClassGraphData(
        val className: String,
        val fullyQualifiedName: String,
        val type: String,
        val dependencies: List<String>,
    )

    companion object {
        val DDD_SYSTEM_PROMPT = """
            You are a domain-driven design expert analyzing a monolithic Java application.

            Your task is to propose a decomposition of this monolith into microservices based on DOMAIN-DRIVEN principles:
            - Identify bounded contexts and domain aggregates
            - Group classes that represent cohesive business capabilities
            - Separate classes based on different business domains (e.g., user management, product catalog, order processing)
            - Consider data ownership and transaction boundaries
            - Minimize cross-service dependencies while maintaining domain cohesion

            Below is the class graph with dependencies:
            {{graphInput}}

            Provide a decomposition proposal with:
            - A unique clusterId for each microservice (e.g., "domain-1", "domain-2", etc.)
            - A descriptive clusterName that reflects the domain/bounded context
            - A description explaining the business capability
            - The list of fully qualified class names form the input graph that belong to this microservice
            - Overall reasoning for your decomposition decisions

            IMPORTANT: Do not modify the fully qualified class names from the input. These are unique identifiers for
            the classes and will be used to map the decomposition back to the original codebase.

            Try to create a decomposition that results in a reasonable number of microservices based on the size of
            the input graph. Try to fit every class from the input graph to one of the microservices.
        """.trimIndent()

        val ADD_SYSTEM_PROMPT = """
            You are a software architecture expert analyzing a monolithic Java application.

            Your task is to propose a decomposition of this monolith into microservices based on ACTOR-DRIVEN principles:
            - Identify different actors/users/personas that interact with the system (e.g., Customer, Admin, Vendor)
            - Group classes and functionality based on which actor primarily uses them
            - Each microservice should serve the needs of a specific actor or user journey
            - Classes used by multiple actors should be carefully evaluated for placement or potential duplication
            - Consider API boundaries from the perspective of actor workflows

            Below is the class graph with dependencies:
            {{graphInput}}

            Provide a decomposition proposal with:
            - A unique clusterId for each microservice (e.g., "actor-1", "actor-2", etc.)
            - A descriptive clusterName that reflects the primary actor or user persona
            - A description explaining which actor this serves and what workflows it supports
            - The list of fully qualified class names that belong to this microservice
            - Overall reasoning for your decomposition decisions
        """.trimIndent()
    }
}
