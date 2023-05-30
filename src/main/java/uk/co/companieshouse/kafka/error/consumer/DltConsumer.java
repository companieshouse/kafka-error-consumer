package uk.co.companieshouse.kafka.error.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import uk.co.companieshouse.kafka.error.config.KafkaErrorProperties;
import uk.gov.companieshouse.logging.Logger;

import java.util.List;
import java.util.Map;

@Component
public class DltConsumer implements ConsumerSeekAware {

    public final static String CONSUMER_ID = "error-consumer";
    public final static String REPLAY_HEADER_MILLIS = "ch-error-topic_replay-millis";

    private final Logger logger;

    private Long endOffset;

    KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    //KafkaErrorProperties kafkaErrorProperties;

    /**
     * Initialise the consumer by connecting to the consumer group
     *
     * @param logger the structured logger
     */
    public DltConsumer(KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry, KafkaErrorProperties kafkaErrorProperties, final Logger logger) {
        this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
        this.kafkaErrorProperties = kafkaErrorProperties;
        if (kafkaErrorProperties.getEndOffset() != null) {
            endOffset = kafkaErrorProperties.getEndOffset();
        }
        this.logger = logger;
    }

    @SendTo("#{kafkaErrorProperties.retryTopic}")
    @KafkaListener(topicPartitions = {
            @org.springframework.kafka.annotation.TopicPartition(topic = "#{kafkaErrorProperties.errorTopic}",
                    partitions = "#{kafkaErrorProperties.partition}")}, id = CONSUMER_ID, groupId = "#{kafkaErrorProperties.consumerGroupId}")
    public Message<Object> listenErrors(ConsumerRecord<String, Object> record,
                                        @Headers Map<String, ?> headers, Consumer<?, ?> consumer) {
        Long offset = (Long) headers.get(KafkaHeaders.OFFSET);
        TopicPartition topicPartition = new TopicPartition(kafkaErrorProperties.getErrorTopic(), kafkaErrorProperties.getPartition());

        if (endOffset == null) {
            endOffset = consumer.endOffsets(List.of(topicPartition)).get(topicPartition) - 1;
        }

        logger.info(" ERROR REPLAY for offset : " + offset);

        if (offset >= endOffset) {
            logger.info("Error consumer has reached offset " + endOffset + " - STOPPING consumer");
            kafkaListenerEndpointRegistry.getListenerContainer(CONSUMER_ID)
                    .pausePartition(topicPartition);
            logger.info("STOPPED consumer");
        }

        return MessageBuilder.withPayload(record.value())
                .setHeader(REPLAY_HEADER_MILLIS, System.currentTimeMillis())
                .build();
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {

        if (kafkaErrorProperties.getStartOffset() != null) {
            assignments.keySet().forEach(partition -> callback.seek(partition.topic(), partition.partition(), kafkaErrorProperties.getStartOffset()));
        }
    }
}
