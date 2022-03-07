package uk.co.companieshouse.kafka.error;

import uk.co.companieshouse.kafka.error.config.KafkaErrorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KafkaErrorProperties.class)
public class KafkaErrorApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaErrorApplication.class, args);
    }

}
