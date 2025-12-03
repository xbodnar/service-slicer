package cz.bodnor.serviceslicer.adapter.out.ai

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.benchmark.port.out.GenerateBehaviorModels
import cz.bodnor.serviceslicer.application.module.file.service.DiskOperations
import cz.bodnor.serviceslicer.domain.operationalsetting.BehaviorModel
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.util.UUID

@Component
class GenerateBehaviorModelsAi(
    chatClientBuilder: ChatClient.Builder,
    private val objectMapper: ObjectMapper,
    private val diskOperations: DiskOperations,
) : GenerateBehaviorModels {

    private val chatClient: ChatClient = chatClientBuilder.build()

    data class AiResponse(
        val behaviorModels: List<BehaviorModel>,
    )

    override fun invoke(openApiFileId: UUID): List<BehaviorModel> = diskOperations.withFile(openApiFileId) {
        val openApiContent = Files.readString(it)

        chatClient.prompt().user { user ->
            user.text(SYSTEM_PROMPT).param("openApiContent", openApiContent)
        }.call().entity(AiResponse::class.java)?.behaviorModels ?: error("Couldn't deserialize OpenAI response")
    }

    companion object {
        val SYSTEM_PROMPT: String = """
            # 🧠 Prompt: Generate Realistic User Behavior Models from OpenAPI

            You are an expert software system analyst and performance engineer.
            Your goal is to generate a **realistic and diverse set of user behavior models** that represent how different types of users interact with a system described by an OpenAPI specification.

            ---

            ## 📘 Input

            You are given an OpenAPI specification document describing all REST endpoints and their `operationId`s.

            Use these to produce realistic `BehaviorModel` objects that can be used for performance and scalability assessments.

            OpenApi specification:
            {openApiContent}

            ---

            ## 🧩 BehaviorModel Schema

            ```kotlin
            data class BehaviorModel(
                val id: String,
                val actor: String,
                val usageProfile: Double,
                val steps: List<String>,
                val thinkFrom: Int,
                val thinkTo: Int,
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
