package uk.co.companieshouse.kafka.error.consumer;

import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import uk.co.companieshouse.kafka.error.config.KafkaErrorProperties;
import uk.gov.companieshouse.logging.Logger;

@Component
public class DltConsumer {

    public final static String CONSUMER_ID = "error-consumer";

    private final Logger logger;

    private Long endOffset;

    @Autowired
    KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    KafkaErrorProperties kafkaErrorProperties;

    /**
     * Initialise the consumer by connecting to the consumer group
     *
     * @param logger the structured logger
     */
    public DltConsumer(final Logger logger) {
        this.logger = logger;
    }

    @SendTo("#{kafkaErrorProperties.retryTopic}")
    @KafkaListener(topicPartitions = {
            @org.springframework.kafka.annotation.TopicPartition(topic = "#{kafkaErrorProperties.errorTopic}",
                    partitions = "0")}, id = CONSUMER_ID, groupId = "#{kafkaErrorProperties.groupId}")
    public Message<Object> listenGroupFoo(ConsumerRecord<String, Object> record,
            @Headers Map<String, ?> headers, Consumer<?, ?> consumer) {
        Long offset = (Long) headers.get(KafkaHeaders.OFFSET);

        if (endOffset == null) {
            TopicPartition tp = new TopicPartition(kafkaErrorProperties.getErrorTopic(), 0);
            endOffset = consumer.endOffsets(List.of(tp)).get(tp) - 1;
        }

        logger.info(" ERROR REPLAY for offset : " + offset);

        if (offset >= endOffset) {
            logger.info("STOPPING listener");
            kafkaListenerEndpointRegistry.getListenerContainer(CONSUMER_ID)
                    .pausePartition(new TopicPartition(kafkaErrorProperties.getErrorTopic(), 0));
            logger.info("STOPPED listener");
        }

        return MessageBuilder.withPayload(record.value())
                .setHeader("ch-error-topic_replay-millis", System.currentTimeMillis())
                .build();

    }

}
