package site.tmdb.tmdbapi

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import reactor.core.publisher.Flux
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@RestController
class SseController(
    private val kafkaProducerService: KafkaProducerService,
    private val kafkaConsumerService: KafkaConsumerService,
    @Value("\${gemini.api.key}")
    private var GEMINI_API_KEY: String
) {

    private val executor = Executors.newSingleThreadExecutor()


    @GetMapping("/sse")
    fun handleSse(@RequestParam prompt: String): SseEmitter {
        val emitter = SseEmitter()
        val clientId = UUID.randomUUID().toString()

        executor.execute {
            try {
                // Send prompt to GPT via Kafka
                kafkaProducerService.sendMessage("gemini-request", clientId, prompt)

                // Stream responses from GPT-4 to Kafka
                val responseFlux: Flux<String> = kafkaProducerService.sendPromptToGpt(prompt)
                responseFlux.subscribe { message ->
                    kafkaProducerService.sendMessage("gemini-response", clientId, message)
                }

                // Stream responses from Kafka to SSE
                val kafkaResponseFlux: Flux<Message> = kafkaConsumerService.getMessages(clientId)
                kafkaResponseFlux
                    .doOnNext { message ->
                        try {
                            emitter.send(SseEmitter.event().data(message.content))
                        } catch (e: IOException) {
                            emitter.completeWithError(e)
                        }
                    }
                    .doOnComplete { emitter.complete() }
                    .doOnError { e -> emitter.completeWithError(e) }
                    .subscribe()
            } catch (e: Exception) {
                emitter.completeWithError(e)
            }
        }

        return emitter
    }
}