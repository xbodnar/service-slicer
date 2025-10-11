package cz.bodnor.serviceslicer.adapter.out.ai

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.microservicesuggestion.communitydetection.CommunityDetectionStrategy
import cz.bodnor.serviceslicer.application.module.microservicesuggestion.port.out.SuggestServiceBoundaryNames
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.openai.api.ResponseFormat
import org.springframework.stereotype.Component

@Component
class SuggestServiceBoundaryNamesAi(
    chatClientBuilder: ChatClient.Builder,
    private val objectMapper: ObjectMapper,
) : SuggestServiceBoundaryNames {

    private val chatClient: ChatClient = chatClientBuilder.build()

    override fun invoke(
        serviceBoundaries: List<CommunityDetectionStrategy.Result.Community>,
    ): SuggestServiceBoundaryNames.Result {
        val outputConverter = BeanOutputConverter(SuggestServiceBoundaryNames.Result::class.java)
        val jsonSchema = outputConverter.jsonSchema

        val prompt = Prompt(
            promptText.replace("{}", objectMapper.writeValueAsString(serviceBoundaries)),
            OpenAiChatOptions.builder()
                .model(OpenAiApi.ChatModel.GPT_4_1_MINI)
                .responseFormat(ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, jsonSchema))
                .build(),
        )

        val response = chatClient.prompt(prompt).call()
        val content = response.content() ?: throw IllegalStateException("No OpenAI response: ${response.content()}")

        val result =
            outputConverter.convert(content) ?: throw NullPointerException("Couldn't deserialize OpenAI response")

        return result
    }
}

val promptText = """
    You are given groups of fully qualified names of Java classes together with an identifier of each group. Each
    group represents a microservice boundary, that was created by decomposing a monolithic application into
    microservices. Based on the list of class names within each group, suggest a service name for each group.
    The name should be short, lower letters with hyphens as separators - conforming to best practices when naming
    services. The name should be usable as a Github repository name for the given service.

    {}
""".trimIndent()
