package site.tmdb.tmdbapi

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@RestController
class SseController(
    private val kafkaProducerService: KafkaProducerService,
    private val kafkaConsumerService: KafkaConsumerService
) {

    private val executor = Executors.newSingleThreadExecutor()

    @Value("\${gpt.api.url}")
    private lateinit var gptApiUrl: String

    @Value("\${gpt.api.key}")
    private lateinit var gptApiKey: String

    @GetMapping("/sse")
    fun handleSse(@RequestParam prompt: String): SseEmitter {
        val emitter = SseEmitter()
        val clientId = UUID.randomUUID().toString()

        executor.execute {
            try {
                // Send prompt to GPT via Kafka
                kafkaProducerService.sendMessage("gpt-request", clientId, prompt)

                // Stream responses from Kafka to SSE
                val responseFlux: Flux<Message> = kafkaConsumerService.getMessages(clientId)
                responseFlux
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