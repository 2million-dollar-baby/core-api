package site.tmdb.tmdbapi

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.util.concurrent.atomic.AtomicLong


@Configuration
class WebClientConfig(
    @Value("\${gemini.api.key}")
    private var GEMINI_API_KEY: String
) {
    private val GEMINI_API_URL = "https://generativelanguage.googleapis.com/"

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .baseUrl(GEMINI_API_URL)
            .build()
    }
}

@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, Message>,
    private val webClient: WebClient,
                           @Value("\${gemini.api.key}")
                           private var GEMINI_API_KEY: String
) {

    private val sequenceNumber = AtomicLong(0)

    fun sendMessage(topic: String, key: String, message: String) {
        val seq = sequenceNumber.incrementAndGet()
        kafkaTemplate.send(topic, key, Message(seq, message))
    }

    fun sendPromptToGpt(prompt: String): Flux<String> {
        return webClient.post()
            .uri("v1/models/gemini-pro:streamGenerateContent?key=${GEMINI_API_KEY}")
            .header("Content-Type", "application/json")
            .bodyValue(GeminiRequest(
                listOf(
                    PartRequest(
                        listOf(
                            ContentRequest(prompt)
                        )
                    )
                )

            ))
            .retrieve()
            .bodyToFlux(String::class.java)
    }
}

data class GeminiRequest(
    @JsonProperty("contents")
    val contents: List<PartRequest>
)

data class PartRequest(
    @JsonProperty("parts")
    val parts: List<ContentRequest>
)

data class ContentRequest(
    @JsonProperty("text")
    val text: String
)

data class Message(
    val sequenceNumber: Long,
    val content: String
)