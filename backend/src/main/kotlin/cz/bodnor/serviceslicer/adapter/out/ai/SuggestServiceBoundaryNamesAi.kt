package cz.bodnor.serviceslicer.adapter.out.ai

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.decomposition.port.out.SuggestServiceBoundaryNames
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.entity
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("integration.service-boundary-name-suggester.mock", havingValue = "false", matchIfMissing = true)
class SuggestServiceBoundaryNamesAi(
    chatClientBuilder: ChatClient.Builder,
    private val objectMapper: ObjectMapper,
) : SuggestServiceBoundaryNames {

    private val chatClient: ChatClient = chatClientBuilder.build()

    private val chatOptions = OpenAiChatOptions.builder().responseFormat(
        ResponseFormat(
            ResponseFormat.Type.JSON_SCHEMA,
            BeanOutputConverter(SuggestServiceBoundaryNamesAiResponse::class.java).jsonSchema,
        ),
    ).build()

    data class SuggestServiceBoundaryNamesAiResponse(
        @get:JsonProperty(required = true)
        val services: List<ServiceNameSuggestion>,
    ) {

        data class ServiceNameSuggestion(
            @get:JsonProperty(required = true)
            val serviceId: String,
            @get:JsonProperty(required = true)
            val primaryName: String,
        )
    }

    override fun invoke(
        services: List<SuggestServiceBoundaryNames.ServiceCluster>,
    ): SuggestServiceBoundaryNames.Result {
        val response = chatClient.prompt(Prompt(objectMapper.writeValueAsString(services), chatOptions))
            .system(promptText)
            .call()
            .entity<SuggestServiceBoundaryNamesAiResponse>()

        return SuggestServiceBoundaryNames.Result(
            serviceNameSuggestions = response.services.associate {
                it.serviceId to it.primaryName
            },
        )
    }
}

val promptText = """
    You are a software architect expert in Domain-Driven Design (DDD), tasked with generating meaningful service names for pre-defined microservice clusters.

    Your task is to analyze the classes in each service cluster and propose a descriptive serviceName based on Domain-Driven Design principles, which focuses on identifying bounded contexts and domain aggregates.

    ## Domain-Driven Naming Principles
    DDD names services by analyzing the business domain and bounded contexts:
    1. Identify the primary domain entity or business capability represented by the classes in each cluster
    2. Name the service after the dominant domain concept
    3. Enable clear understanding of which business capability each service owns

    ## Domain Identification Heuristics
    Look for these patterns in class names to identify domains:
    - **Entity patterns**: *Entity, *Model, *Aggregate, *Root
    - **Repository patterns**: *Repository, *Dao, *Store
    - **Service patterns**: *Service, *Handler, *Manager, *Processor
    - **Controller patterns**: *Controller, *Resource, *Endpoint, *Api
    - **DTO patterns**: *Dto, *Request, *Response, *Command, *Query

    Extract the domain concept from class names by removing these suffixes:
    - `UserController`, `UserService`, `UserRepository` → **User** domain
    - `OrderEntity`, `OrderService`, `OrderDto` → **Order** domain
    - `PaymentProcessor`, `PaymentHandler` → **Payment** domain

    ## Naming Convention Rules
    - serviceName MUST use camelCase with no spaces (e.g., "userManagement", "orderProcessing", "productCatalog", "commonLibrary")
    - Do NOT use spaces or hyphens in names
    - Name services after the PRIMARY/DOMINANT domain, not concatenated domain names

    ### Naming Strategy
    1. **Identify all domain concepts**: Extract domain names from class names by removing technical suffixes
    2. **Count occurrences**: Determine which domain has the most classes in the cluster
    3. **Use the dominant domain for the name**: Select the single domain with the highest class count
    4. **If domains have similar counts**: Choose the more business-critical domain or use a higher-level abstraction that encompasses related domains

    ### Higher-Level Abstractions for Related Domains
    When a cluster contains multiple related domains with similar class counts, use an encompassing abstraction:
    - User + Profile + Account → "identityManagement"
    - Article + Comment + Post → "contentManagement"
    - Order + Payment + Invoice → "orderManagement" (Order is typically the aggregate root)
    - Product + Category + Inventory → "catalogManagement"
    - Auth + Security + Permission → "securityService"
    - Notification + Email + Sms → "notificationService"
    - Shared utilities across domains → "commonLibrary" or "sharedCore"

    ### Examples
    - A cluster with UserController, UserService, UserRepository, ProfileDto → "userManagement"
    - A cluster with OrderEntity, OrderService, PaymentHandler, InvoiceDto → "orderManagement" (Order is dominant/aggregate root)
    - A cluster with ArticleController (3 classes), CommentService (2 classes) → "articleManagement" (Article is dominant)
    - A cluster with AuthService, SecurityFilter, PermissionValidator → "securityService"
    - A cluster with utility classes, helpers, common exceptions → "commonLibrary"

    ### Anti-patterns to Avoid
    - ❌ "userProfileAccountService" (concatenated domains)
    - ❌ "orderPaymentService" (concatenated domains)
    - ❌ "User Management" (spaces)
    - ❌ "user-management" (hyphens)
    - ✅ "userManagement" (single domain, camelCase)
    - ✅ "orderProcessing" (single domain, camelCase)
    - ✅ "identityManagement" (higher-level abstraction, camelCase)

    Each cluster has:
    - `id`: A unique identifier for the service cluster
    - `classes`: A list of fully qualified class names belonging to this cluster

    ## Required Output
    Provide a JSON object with a `services` field containing a list of `ServiceNameSuggestion` objects:
    - `serviceId`: Must exactly match the `id` from the input cluster
    - `serviceName`: A proposed name in camelCase reflecting the primary domain or bounded context

    Example output format:
    ```json
    {
      "services": [
        {
          "serviceId": "cluster-1",
          "serviceName": "userManagement"
        },
        {
          "serviceId": "cluster-2",
          "serviceName": "orderProcessing"
        },
        {
          "serviceId": "cluster-3",
          "serviceName": "commonLibrary"
        }
      ]
    }
    ```

    ## Validation Checklist (verify before responding)
    - [ ] Every serviceId in the output exactly matches an id from the input
    - [ ] All serviceNames are in camelCase with no spaces or hyphens
    - [ ] No serviceName contains concatenated domain names (e.g., avoid "orderPaymentInvoiceService")
    - [ ] Each serviceName reflects the primary domain or a clear higher-level abstraction
    - [ ] Every input cluster has exactly one corresponding ServiceNameSuggestion in the output
""".trimIndent()
