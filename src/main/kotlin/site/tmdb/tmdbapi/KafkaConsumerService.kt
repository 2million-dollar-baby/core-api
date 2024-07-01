package site.tmdb.tmdbapi

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue


@Service
class KafkaConsumerService {

    private val listeners = ConcurrentHashMap<String, FluxSink<Message>>()
    private val messageQueues = ConcurrentHashMap<String, PriorityBlockingQueue<Message>>()

    @KafkaListener(topics = ["gemini-response"], groupId = "group_id")
    fun consume(key: String, message: Message) {
        val queue = messageQueues.computeIfAbsent(key) {
            PriorityBlockingQueue(compareBy { it.sequenceNumber }) }
        queue.add(message)

        listeners[key]?.let { sink ->
            var nextMessage: Message?
            while (queue.poll().also { nextMessage = it } != null) {
                sink.next(nextMessage!!)
            }
        }
    }

    fun getMessages(clientId: String): Flux<Message> {
        return Flux.create { sink ->
            listeners[clientId] = sink.onDispose { listeners.remove(clientId) }
        }
    }
}