package uk.co.companieshouse.kafka.error.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import uk.co.companieshouse.kafka.error.config.KafkaErrorProperties;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static uk.co.companieshouse.kafka.error.consumer.TestConsumer.TEST_CONSUMER_ID;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092",
        "port=9092"})
public class DltConsumerEmbeddedKafkaTest {

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

        kafkaTemplate.send(kafkaErrorProperties.getErrorTopic(), "{hello: world}".getBytes());

        kafkaListenerEndpointRegistry.getListenerContainer(TEST_CONSUMER_ID).start();

        consumer.getLatch().await(10000, TimeUnit.MILLISECONDS);

        assertThat(consumer.getLatch().getCount(), equalTo(0L));
        assertThat(consumer.getPayload().headers().lastHeader("ch-error-topic_replay-millis"),
                is(notNullValue()));
    }
}
