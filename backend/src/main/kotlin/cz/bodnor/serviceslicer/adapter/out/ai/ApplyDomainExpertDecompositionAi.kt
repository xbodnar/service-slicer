package cz.bodnor.serviceslicer.adapter.out.ai

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.analysis.port.out.ApplyDomainExpertDecomposition
import cz.bodnor.serviceslicer.application.module.decomposition.command.DomainExpertDecompositionCommand.DomainDecompositionType
import cz.bodnor.serviceslicer.domain.graph.ClassNode
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.api.Advisor
import org.springframework.ai.chat.client.entity
import org.springframework.ai.chat.client.responseEntity
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

    private val chatOptions = OpenAiChatOptions.builder().responseFormat(
        ResponseFormat(
            ResponseFormat.Type.JSON_SCHEMA,
            BeanOutputConverter(ApplyDomainExpertDecompositionAiResponse::class.java).jsonSchema,
        ),
    ).build()

    data class ApplyDomainExpertDecompositionAiResponse(
        @get:JsonProperty(required = true)
        val services: List<Service>,
    ) {

        data class Service(
            @get:JsonProperty(required = true)
            val serviceId: String,
            @get:JsonProperty(required = true)
            val serviceName: String,
            @get:JsonProperty(required = true)
            val classes: List<String>,
        )
    }

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

        val response = chatClient.prompt(Prompt(objectMapper.writeValueAsString(promptInput), chatOptions))
            .system(rawPrompt)
            .call()
            .entity<ApplyDomainExpertDecompositionAiResponse>()

        return ApplyDomainExpertDecomposition.Result(
            microservices = response.services.map { service ->
                ApplyDomainExpertDecomposition.MicroserviceCluster(
                    clusterId = service.serviceId,
                    clusterName = service.serviceName,
                    classes = service.classes,
                )
            },
        )
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

            Your task is to propose a PRAGMATIC decomposition of this monolith into microservices based on Domain-Driven Design principles.

            ## Core Principles
            - Identify bounded contexts and domain aggregates
            - Group classes that represent cohesive business capabilities
            - Separate classes based on different business domains (e.g., user management, product catalog, order processing)
            - Consider data ownership and transaction boundaries
            - Minimize cross-service dependencies while maintaining domain cohesion

            ## Critical Constraints

            ### Service Count Guidelines
            - Target approximately 1 microservice per 15-25 classes in the input graph
            - For a monolith with ~80 classes, aim for 3-6 microservices maximum
            - NEVER create a microservice with fewer than 5 classes unless there is an exceptionally strong domain justification
            - If a potential service has only 1-3 classes, merge it with the most closely related service
            - Prefer fewer, larger services over many small ones—microservices should represent substantial business capabilities, not fine-grained technical concerns

            ### Service Granularity Rules
            1. Controllers, services, repositories, DTOs, and entities that operate on the same domain entity MUST be grouped together
            2. Utility classes, helpers, and cross-cutting concerns should be assigned to the service that uses them most, or to a "common/core/shared" service or library if truly shared
            3. Authentication, authorization, and security classes typically belong together in one service
            4. Configuration classes belong with the domain they configure

            ### Naming Convention
            - serviceName MUST use camelCase with no spaces (e.g., "userService", "orderProcessing", "productCatalog", "inventoryManagement")
            - Do NOT use spaces or hyphens in names
            - When a microservice contains multiple domain entities, name it after the PRIMARY/DOMINANT domain only:
              - Identify which domain has the most classes in the service
              - Use that single domain for the name
              - Do NOT concatenate multiple domain names together
              - Examples:
                - A service with User (8 classes), Profile (3 classes), Tag (2 classes) → name it "userService", NOT "userProfileTagService"
                - A service with Order (10 classes), Payment (4 classes) → name it "orderManagement", NOT "orderPaymentManagement"
                - A service with Article (6 classes), Comment (5 classes) → name it "articleManagement" (or "contentManagement" as an abstraction)
              - If two domains have similar class counts, choose the more business-critical domain or use a higher-level abstraction that encompasses both (e.g., "contentManagement" for Article+Comment, "identityManagement" for Auth+Security)

            ## Required Output
            Provide a decomposition proposal with:
            - A unique serviceId for each microservice (e.g., "domain-1", "domain-2", etc.)
            - A descriptive serviceName in camelCase reflecting the bounded context (e.g., "userService", "orderProcessing", "notificationDispatcher", "commonLibrary")
            - The list of fully qualified class names from the input graph that belong to this microservice

            ## Validation Checklist (verify before responding)
            - [ ] No service has fewer than 5 classes (unless explicitly justified)
            - [ ] All serviceNames are in camelCase with no spaces
            - [ ] Every class from the input is assigned to exactly one service
            - [ ] Related technical layers (controller, service, repository, entity) for the same domain are in the same microservice

            IMPORTANT: Do not modify the fully qualified class names from the input. These are unique identifiers for the classes and will be used to map the decomposition back to the original codebase.
        """.trimIndent()

        val ADD_SYSTEM_PROMPT = """
            You are a software architect expert in Actor-Driven Design (ADD), analyzing a monolithic Java application.

            Your task is to propose a PRAGMATIC decomposition of this monolith into microservices based on Actor-Driven Design principles, which refines traditional Domain-Driven Design by incorporating actors' runtime usage patterns.

            ## Actor-Driven Design Principles
            ADD decomposes services by analyzing WHO uses WHAT operations and with WHAT intensity:
            1. Identify actors (user roles) interacting with the system
            2. Map operations to the actors that invoke them
            3. Build actor-operation coverings: group operations by shared actor usage patterns
            4. Split bounded contexts according to these usage-based groupings
            5. Enable selective scaling of high-load operation groups

            ## Decomposition Strategy
            When analyzing the class graph:
            1. **Identify implicit actors** from class names and patterns:
               - Admin/Administrator classes → Admin actor
               - User/Customer/Client classes → EndUser actor
               - Public/External/Guest classes → External actor
               - API/Integration classes → System actor
               - Look for role-specific prefixes/suffixes (e.g., AdminController, UserService, PublicAPI)

            2. **Build actor-operation mappings**:
               - Group classes by which actor would primarily use them
               - Controllers, services, repositories handling the same actor's operations belong together
               - Consider read vs. write patterns (CRUD decomposition): Admin actors often perform CUD, EndUsers often perform R

            3. **Apply covering rules**:
               - If two actors use completely disjoint operation sets → separate services
               - If actors share some operations → consider replication, merging, or dedicated shared service
               - High-frequency actor operations should be isolatable for independent scaling

            ## Critical Constraints

            ### Service Count Guidelines
            - Target approximately 1 microservice per 15-25 classes in the input graph
            - For a monolith with ~80 classes, aim for 3-6 microservices maximum
            - NEVER create a microservice with fewer than 5 classes unless there is an exceptionally strong actor-based justification
            - If a potential service has only 1-3 classes, merge it with the service serving the same or most similar actor
            - Prefer fewer, larger services over many small ones—microservices should represent substantial actor-bounded capabilities

            ### Service Granularity Rules
            1. Classes serving the SAME ACTOR and operating on the same domain entity MUST be grouped together
            2. Utility classes, helpers, and cross-cutting concerns should be assigned to the service of the actor that uses them most, or to a "commonLibrary" if truly shared across actors
            3. Authentication, authorization, and security classes typically form an "identityManagement" or "authService" serving all actors
            4. Configuration classes belong with the actor-specific service they configure

            ### Naming Convention
            - serviceName MUST use camelCase with no spaces (e.g., "adminOperations", "endUserService", "externalApi", "commonLibrary")
            - Do NOT use spaces or hyphens in names
            - Name services after the PRIMARY ACTOR they serve, not the domain entities:
              - Identify which actor primarily uses the service
              - Use that actor for the name
              - Do NOT concatenate multiple actor names together
              - Examples:
                - A service with Admin operations (8 classes), some shared utilities (3 classes) → name it "adminService", NOT "adminUtilityService"
                - A service with EndUser read operations (10 classes), Customer support (4 classes) → name it "endUserService"
                - A service with External API (6 classes), Public access (5 classes) → name it "externalApi" (or "publicApi")
              - If a service genuinely serves multiple actors equally, use a functional abstraction (e.g., "queryService" for read-heavy operations across actors, "commandService" for write operations)

            ## Actor Identification Heuristics
            Look for these patterns in class names to identify actors:
            - **Admin actor**: Admin*, *Admin, *Management, *Dashboard, *BackOffice, *Internal
            - **EndUser actor**: User*, Customer*, Client*, *Account, *Profile, *Personal
            - **External actor**: Public*, External*, Guest*, *Api, *Integration, *Webhook
            - **System actor**: Scheduler*, Job*, Batch*, *Task, *Worker, *Background

            ## Required Output
            Provide a decomposition proposal with:
            - A unique serviceId for each microservice (e.g., "actor-1", "actor-2", etc.)
            - A descriptive serviceName in camelCase reflecting the primary actor served (e.g., "adminService", "endUserOperations", "publicApi", "commonLibrary")
            - The list of fully qualified class names from the input graph that belong to this microservice

            ## Validation Checklist (verify before responding)
            - [ ] Each service primarily serves one actor or a clearly defined actor group
            - [ ] No service has fewer than 5 classes (unless explicitly justified by actor isolation needs)
            - [ ] All serviceNames are in camelCase with no spaces
            - [ ] Service names reflect the primary actor, not concatenated actor names
            - [ ] Every class from the input is assigned to exactly one service
            - [ ] Classes serving the same actor's operations are grouped together
            - [ ] High-frequency operations (typically EndUser/External) are isolatable for scaling

            IMPORTANT: Do not modify the fully qualified class names from the input. These are unique identifiers for the classes and will be used to map the decomposition back to the original codebase.
        """.trimIndent()
    }
}
