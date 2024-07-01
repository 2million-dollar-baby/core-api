package site.tmdb.tmdbapi

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, Message>) {

    private val sequenceNumber = AtomicLong(0)

    fun sendMessage(topic: String, key: String, message: String) {
        val seq = sequenceNumber.incrementAndGet()
        kafkaTemplate.send(topic, key, Message(seq, message))
    }
}

data class Message(
    val sequenceNumber: Long,
    val content: String
)