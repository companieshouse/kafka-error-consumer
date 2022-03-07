package uk.co.companieshouse.kafka.error.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = KafkaErrorProperties.class)
@TestPropertySource(properties = {
        "kafka.error.retryTopic=testRetryTopic",
        "kafka.error.errorTopic=testErrorTopic",
        "kafka.error.consumerGroupId=testConsumerGroupId"
})
public class KafkaErrorPropertiesTest {

    @Autowired
    KafkaErrorProperties kafkaErrorProperties;

    @Test
    void contextLoads() throws Exception {

        assertThat(kafkaErrorProperties, is(notNullValue()));
        assertThat(kafkaErrorProperties.getErrorTopic(), is("testErrorTopic"));
        assertThat(kafkaErrorProperties.getRetryTopic(), is("testRetryTopic"));
        assertThat(kafkaErrorProperties.getConsumerGroupId(), is("testConsumerGroupId"));
        assertThat(kafkaErrorProperties.getBootstrapAddress(), is("localhost:9092"));

    }
}
