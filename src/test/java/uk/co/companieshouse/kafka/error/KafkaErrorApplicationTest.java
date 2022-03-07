package uk.co.companieshouse.kafka.error;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.co.companieshouse.kafka.error.consumer.DltConsumer;

@SpringBootTest
@ActiveProfiles("test")
// mock DltConsumer so it doesn't start listening to kafka
@MockBean({DltConsumer.class})
public class KafkaErrorApplicationTest {

    @Test
    void contextLoads() throws Exception {
    }
}
