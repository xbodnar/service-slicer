package cz.bodnor.serviceslicer.adapter.out.ai

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.benchmark.port.out.GenerateUsageProfile
import cz.bodnor.serviceslicer.application.module.file.service.DiskOperations
import cz.bodnor.serviceslicer.domain.operationalsetting.ApiRequest
import cz.bodnor.serviceslicer.domain.operationalsetting.BehaviorModel
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.entity
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.nio.file.Files
import java.util.UUID

@Component
class GenerateUsageProfileAi(
    chatClientBuilder: ChatClient.Builder,
    private val objectMapper: ObjectMapper,
    private val diskOperations: DiskOperations,
) : GenerateUsageProfile {

    private val chatClient: ChatClient = chatClientBuilder.build()

    private val chatOptions = OpenAiChatOptions.builder().responseFormat(
        ResponseFormat(
            ResponseFormat.Type.JSON_SCHEMA,
            BeanOutputConverter(AiResponse::class.java).jsonSchema,
        ),
    ).build()

    override fun invoke(openApiFileId: UUID): List<BehaviorModel> {
        val openApiSpecification = diskOperations.withFile(openApiFileId) { Files.readString(it) }

        val response = chatClient.prompt(Prompt(openApiSpecification, chatOptions))
            .system { SYSTEM_PROMPT }.call().entity<AiResponse>()

        return response.usageProfile.map { behaviorModelDto ->
            BehaviorModel(
                id = behaviorModelDto.id,
                actor = behaviorModelDto.actor,
                frequency = behaviorModelDto.frequency,
                steps = behaviorModelDto.steps.map {
                    ApiRequest(
                        operationId = it.operationId,
                        method = it.method,
                        path = it.path,
                        component = it.component,
                        headers = it.headers,
                        params = it.params,
                        body = it.body,
                        save = it.save,
                        waitMsFrom = it.waitMsFrom,
                        waitMsTo = it.waitMsTo,
                    )
                },
            )
        }
    }

    data class AiResponse(
        @get:JsonProperty(required = true)
        val usageProfile: List<BehaviorModelDto>,
    )

    data class BehaviorModelDto(
        @get:JsonProperty(required = true)
        val id: String,
        @get:JsonProperty(required = true)
        val actor: String,
        @get:JsonProperty(required = true)
        val frequency: BigDecimal,
        @get:JsonProperty(required = true)
        val steps: List<ApiRequestDto>,
    )

    data class ApiRequestDto(
        @get:JsonProperty(required = true)
        val operationId: String,
        @get:JsonProperty(required = true)
        val method: String,
        @get:JsonProperty(required = true)
        val path: String,
        @get:JsonProperty(required = false)
        val component: String? = null,
        @get:JsonProperty(required = false)
        val headers: Map<String, String> = emptyMap(),
        @get:JsonProperty(required = false)
        val params: Map<String, String> = emptyMap(),
        @get:JsonProperty(required = false)
        val body: Map<String, Any?> = emptyMap(),
        @get:JsonProperty(required = false)
        val save: Map<String, String> = emptyMap(),
        @get:JsonProperty(required = true)
        val waitMsFrom: Int,
        @get:JsonProperty(required = true)
        val waitMsTo: Int,
    )

    companion object {
        val SYSTEM_PROMPT: String = """
            # 🧠 Prompt: Generate Realistic User Behavior Models from OpenAPI

            You are an expert software system analyst and performance engineer.
            Your goal is to generate a **realistic and diverse set of user behavior models** that represent how different types of users interact with a system described by an OpenAPI specification.

            ---

            ## 📘 Input

            You are given an OpenAPI specification document describing all REST endpoints and their `operationId`s.

            Use these to produce realistic `BehaviorModel` objects that can be used for performance and scalability assessments.

            ---

            ## 🧩 BehaviorModel Schema

            ```kotlin
            /**
            * Represents a user behavior model (flow/scenario).
            */
            data class BehaviorModel(
                // Arbitrary ID such as bm1, bm2 so users can identify this behavior model
                val id: String,
                // Name of the actor persona this behavior model represents
                val actor: String,
                // Probability for this behaviour model. Must be between 0 and 1 and sum of frequencies
                // across all behavior models must be 1.
                val frequency: BigDecimal,
                // Sequence of API operation that comprise this user flow
                val steps: List<ApiRequest>,
            )

            /**
             * Represents a single API request within a behavior model.
             */
            data class ApiRequest(
                /**
                 * ID of the API operation taken from the OpenApi specification, that this request corresponds to.
                 */
                val operationId: String,
                /**
                 * HTTP method of the API operation taken from the OpenApi specification, that this request corresponds to.
                 */
                val method: String,
                /**
                 * Path of the API operation taken from the OpenApi specification, that this request corresponds to.
                 */
                val path: String,
                /**
                 * Name of the component this request belongs to.
                 */
                val component: String? = null,
                /**
                 * Map of HTTP Headers (key-value pairs), may contain variables
                 */
                val headers: Map<String, String> = emptyMap(),
                /**
                 * Map of Query Parameters (key-value pairs), may contain variables
                 */
                val params: Map<String, String> = emptyMap(),
                /**
                 * Request body (JSON Object), may contain variables
                 */
                val body: Map<String, Any?> = emptyMap(),
                /**
                 * Map of JSONPath selectors to extract variables from the response. Key is the variable name, value is the
                 * JSONPath selector. If a subsequent requests are using any variables, it must be defined in a previous step.
                 * Example: {"articleId": "$.articles[0].id"}
                 */
                val save: Map<String, String> = emptyMap(),
                /**
                 * Minimum think time in milliseconds
                 */
                val waitMsFrom: Int,
                /**
                 * Maximum think time in milliseconds
                 */
                val waitMsTo: Int,
            )
            ```

            ---

            ## 🧠 Instructions

            ### 1) Understand the API context

            * Read the OpenAPI specification.
            * Identify **main user roles or personas** based on authentication scopes, endpoint tags, or resource semantics (e.g., "Admin", "Reader", "Writer", "Customer", "PartnerAPI").
            * Infer typical, realistic actions each persona would perform in a normal session.

            ### 2) Define multiple behavior models per actor when appropriate

            * Each `BehaviorModel` represents a **distinct usage pattern** for the same actor.
            * Example:
              * `Reader-Passive`: only views articles.
              * `Reader-Social`: reads and also comments/follows.
            * This models real diversity within a role.

            ### 3) Construct each behavior model with

            * `id`: short unique ID (e.g., `"reader-passive"`, `"reader-social"` …).
            * `actor`: persona name (from Step 1).
            * `usageProfile`: estimated share of total traffic for this flow (0–1; all models together must sum to 1).
            * `steps`: realistic **sequence of operation IDs** (`operationId` from the OpenAPI spec) modeling one coherent session (recommend 3–10 operations).
            * `thinkFrom` / `thinkTo`: **think-time range in milliseconds** (user delay between actions).
              * Human actors: typically 1000–5000 ms.
              * Automated/system actors: typically 50–500 ms.

            ### 4) Ensure realism and coverage

            * Include both **common** and **rare** flows.
            * Include at least one model for each **major actor**.
            * Include both **read-heavy** and **write-heavy** flows if applicable.
            * Keep logical ordering — avoid impossible sequences (e.g., deleting before creating).
            * Vary `usageProfile` values to represent likely traffic proportions.

            ---

            ## ✅ Validation Rules

            * The sum of all `usageProfile` values **must equal 1.0** (±0.01 tolerance).
            * Every entry in `steps` **must** reference a valid `operationId` from the provided OpenAPI file.
            * Each `BehaviorModel` must contain **3–10 steps**.
            * `thinkFrom` and `thinkTo` must be positive and realistic (`thinkFrom` ≤ `thinkTo`).
            * There must be **at least one behavior per major actor**.

            ---

            ## ⚙️ Style and Naming

            * Use simple, clear actor names and short flow IDs.
            * Keep `usageProfile` values proportional to realistic traffic.
            * Vary step counts and think times slightly between behaviors to reflect human diversity.

            ---

            ## 🎯 Goal

            Produce a **single valid JSON array** of realistic `BehaviorModel` objects that:

            * Reflects typical and diverse user interactions with the API.
            * Respects the schema above.
            * Can be directly used for scalability assessment and workload simulation.
        """.trimIndent()
    }
}
