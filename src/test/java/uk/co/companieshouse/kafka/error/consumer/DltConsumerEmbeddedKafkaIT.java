package uk.co.companieshouse.kafka.error.consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static uk.co.companieshouse.kafka.error.consumer.TestConsumer.TEST_CONSUMER_ID;

import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import uk.co.companieshouse.kafka.error.config.KafkaErrorProperties;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"test-error", "test-retry"},
        bootstrapServersProperty = "kafka.error.bootstrapAddress")
@DirtiesContext
@EnableConfigurationProperties(value = KafkaErrorProperties.class)
class DltConsumerEmbeddedKafkaIT {

    @Autowired
    private TestConsumer consumer;

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Autowired
    KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    KafkaErrorProperties kafkaErrorProperties;

    @Test
    void testConsumer() throws Exception {

        byte[] payload = "{hello: world}".getBytes(StandardCharsets.UTF_8);
        kafkaTemplate.send(kafkaErrorProperties.getErrorTopic(), payload).get(10, TimeUnit.SECONDS);

        kafkaListenerEndpointRegistry.getListenerContainer(TEST_CONSUMER_ID).start();

        consumer.getLatch().await(10000, TimeUnit.MILLISECONDS);

        assertThat(consumer.getLatch().getCount(), equalTo(0L));
        assertThat(consumer.getPayload().headers().lastHeader("ch-error-topic_replay-millis"),
                is(notNullValue()));
        assertArrayEquals(payload, (byte[]) consumer.getPayload().value());
    }
}
