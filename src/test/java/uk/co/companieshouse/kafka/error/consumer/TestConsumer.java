package uk.co.companieshouse.kafka.error.consumer;


import java.util.concurrent.CountDownLatch;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import uk.co.companieshouse.kafka.error.config.KafkaErrorProperties;

/**
 * Test consumer which reads from the retry topic to check that records have been moved.
 */
@Component
public class TestConsumer {

    public final static String TEST_CONSUMER_ID = "test-consumer";

    @Autowired
    KafkaErrorProperties kafkaErrorProperties;

    private final CountDownLatch latch = new CountDownLatch(1);
    private ConsumerRecord<String, Object> payload = null;

    @KafkaListener(topics = "#{kafkaErrorProperties.retryTopic}", id = TEST_CONSUMER_ID, autoStartup = "false")
    public void receive(ConsumerRecord<String, Object> consumerRecord) {
        payload = consumerRecord;
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public ConsumerRecord<String, Object> getPayload() {
        return payload;
    }
}


