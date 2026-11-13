package uk.co.companieshouse.kafka.error.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import uk.co.companieshouse.kafka.error.config.KafkaErrorProperties;

class ApplicationConfigurationIT extends KafkaIntegrationSupport {

    @BeforeAll
    static void clearEmbeddedKafkaProperty() {
        // set by EmbeddedKafka in DltConsumerEmbeddedKafkaIT and not cleared afterwards
        System.clearProperty("kafka.error.bootstrapAddress");
    }

    private final ApplicationContextRunner configuration = new ApplicationContextRunner()
            .withUserConfiguration(BindingConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(KafkaErrorProperties.class)
    static class BindingConfiguration {
    }

    @Test
    void bindsDeploymentEnvironmentVariables() {
        Map<String, Object> environment = new HashMap<>(Map.of(
                "KAFKA_ERROR_ERROR_TOPIC", errorTopic,
                "KAFKA_ERROR_RETRY_TOPIC", retryTopic,
                "KAFKA_ERROR_CONSUMER_GROUP_ID", groupId,
                "KAFKA_ERROR_BOOTSTRAP_ADDRESS", KAFKA.getBootstrapServers(),
                "KAFKA_ERROR_START_OFFSET", "1",
                "KAFKA_ERROR_END_OFFSET", "3",
                "KAFKA_ERROR_PARTITION", "1"));
        configuration.withInitializer(context -> context.getEnvironment().getPropertySources()
                        .addFirst(new SystemEnvironmentPropertySource("deployment-test", environment)))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(KafkaErrorProperties.class);
                    assertThat(properties.getErrorTopic()).isEqualTo(errorTopic);
                    assertThat(properties.getRetryTopic()).isEqualTo(retryTopic);
                    assertThat(properties.getConsumerGroupId()).isEqualTo(groupId);
                    assertThat(properties.getBootstrapAddress()).isEqualTo(KAFKA.getBootstrapServers());
                    assertThat(properties.getStartOffset()).isEqualTo(1L);
                    assertThat(properties.getEndOffset()).isEqualTo(3L);
                    assertThat(properties.getPartition()).isEqualTo(1);
                });
    }

    @ParameterizedTest
    @CsvSource({"errorTopic,missing", "errorTopic,blank", "retryTopic,missing", "retryTopic,blank"})
    void rejectsMissingOrBlankMandatoryTopic(String property, String condition) {
        String otherProperty = property.equals("errorTopic") ? "retryTopic" : "errorTopic";
        var runner = configuration.withPropertyValues("kafka.error." + otherProperty + "=valid-topic");
        if (condition.equals("blank")) {
            runner = runner.withPropertyValues("kafka.error." + property + "= ");
        }
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
        });
    }

    @Test
    void retainsDefaultsWhenOptionalPropertiesAreOmitted() {
        configuration.withPropertyValues("kafka.error.errorTopic=" + errorTopic,
                        "kafka.error.retryTopic=" + retryTopic)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(KafkaErrorProperties.class);
                    assertThat(properties.getStartOffset()).isNull();
                    assertThat(properties.getEndOffset()).isNull();
                    assertThat(properties.getPartition()).isZero();
                    assertThat(properties.getConsumerGroupId()).isEqualTo("kafka-error");
                    assertThat(properties.getBootstrapAddress()).isEqualTo("localhost:9092");
                });
    }

    @Test
    void healthEndpointStaysUpAfterReplayPauses() throws Exception {
        seed(0, "healthcheck-replay");
        startApplication(0L, 0L, 0, WebApplicationType.SERVLET, offset -> { });
        startReplay();
        assertPayloads(read(1), "healthcheck-replay");
        awaitPaused(0);
        awaitCommitted(0, 1);

        String port = application.getEnvironment().getRequiredProperty("local.server.port");
        try (HttpClient client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:" + port + "/kafka-error-consumer/healthcheck"))
                            .timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"status\":\"UP\"").doesNotContain("\"components\"");
        }
        assertNoMoreRecords();
    }
}
