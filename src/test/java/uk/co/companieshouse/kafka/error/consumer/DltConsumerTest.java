package uk.co.companieshouse.kafka.error.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import uk.co.companieshouse.kafka.error.config.KafkaErrorProperties;
import uk.gov.companieshouse.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;
import static uk.co.companieshouse.kafka.error.consumer.DltConsumer.CONSUMER_ID;
import static uk.co.companieshouse.kafka.error.consumer.DltConsumer.REPLAY_HEADER_MILLIS;

@ExtendWith(MockitoExtension.class)
public class DltConsumerTest {

    private final static String TOPIC = "topic";

    @Mock
    KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Spy
    KafkaErrorProperties kafkaErrorProperties = new KafkaErrorProperties();

    @Mock
    Consumer<?, ?> consumer;

    @Mock
    Logger logger;

    @Mock
    MessageListenerContainer messageListenerContainer;

    @InjectMocks
    private DltConsumer dltConsumer;

    @Test
    void testConsumer() throws Exception {
        executeTest(10L, 100L, 1, 0);
        executeTest(100L, 100L, 0, 1);
    }

    private void executeTest(Long messageOffset, Long topicoffset, Integer expectedOffset, Integer expectedPause) {

        ConsumerRecord<String, Object> record = new ConsumerRecord<>(TOPIC, 0, 1, "key", "value");
        Map<String, Object> headers = new HashMap<>();
        headers.put(KafkaHeaders.OFFSET, messageOffset);

        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);

        Map<TopicPartition, Long> offsets = Map.of(topicPartition, topicoffset);

        if (expectedOffset > 0) {
            when(consumer.endOffsets(List.of(topicPartition))).thenReturn(offsets);
        } else {
            reset(consumer);
        }
        if (expectedPause > 0) {
            when(kafkaListenerEndpointRegistry.getListenerContainer(CONSUMER_ID)).thenReturn(messageListenerContainer);
        }
        kafkaErrorProperties.setErrorTopic(TOPIC);

        Message<Object> result = dltConsumer.listenErrors(record, headers, consumer);

        assertThat(result, is(notNullValue()));
        assertThat(result.getHeaders(), is(notNullValue()));
        assertThat(result.getHeaders().get(REPLAY_HEADER_MILLIS), is(notNullValue()));

        verify(consumer, times(expectedOffset)).endOffsets(List.of(topicPartition));
        verify(messageListenerContainer, times(expectedPause)).pausePartition(topicPartition);

    }
}
