package uk.co.companieshouse.kafka.error;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.companieshouse.kafka.error.consumer.DltConsumer;

@SpringBootTest
@ActiveProfiles("test")
// mock DltConsumer so it doesn't start listening to kafka
@MockitoBean(types = {DltConsumer.class})
public class KafkaErrorApplicationTest {

    @Test
    void contextLoads() throws Exception {
    }
}
