package uk.co.companieshouse.kafka.error.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import uk.gov.companieshouse.logging.Logger;

/**
 * Configuration required for creating the consumers
 * Requires configuration beans from KafkaSerializerConfig
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Autowired
    KafkaErrorProperties kafkaErrorProperties;

    @Autowired
    private Logger logger;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Bean
    public ConsumerFactory<String, byte[]> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaErrorProperties.getBootstrapAddress());
        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                kafkaErrorProperties.getConsumerGroupId());
        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class);
        props.put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                1);
        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");

        return new DefaultKafkaConsumerFactory(props, new StringDeserializer(),
                new ByteArrayDeserializer());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // used for @SendTo
        factory.setReplyTemplate(kafkaTemplate);

        return factory;
    }
}
