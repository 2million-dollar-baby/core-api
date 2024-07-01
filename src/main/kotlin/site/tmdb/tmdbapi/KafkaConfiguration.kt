package site.tmdb.tmdbapi

import com.fasterxml.jackson.databind.ser.std.StringSerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory


@Configuration
class KafkaConfiguration(
    @Value("\${spring.kafka.producer.bootstrap-servers}")
    private var bootstrapServers: String,
) {

    @Bean
    fun producerFactory() : ProducerFactory<String, String> {
        val configs: HashMap<String, Any> = hashMapOf()

        configs[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        configs[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configs[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        return DefaultKafkaProducerFactory(configs)
    }

    fun kafkaTemplate() : KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory());
    }
}