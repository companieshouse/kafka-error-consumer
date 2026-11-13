package uk.co.companieshouse.kafka.error.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OpenTelemetryAppenderInitializerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(OpenTelemetry.class, () -> mock(OpenTelemetry.class))
            .withUserConfiguration(OpenTelemetryAppenderInitializer.class);

    @Test
    void installsAppenderWhenEnabled() {
        OpenTelemetry openTelemetry = mock(OpenTelemetry.class);
        try (MockedStatic<OpenTelemetryAppender> appender = Mockito.mockStatic(OpenTelemetryAppender.class)) {
            new OpenTelemetryAppenderInitializer(openTelemetry).afterPropertiesSet();
            appender.verify(() -> OpenTelemetryAppender.install(openTelemetry));
        }
    }

    @Test
    void beanAbsentByDefault() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(OpenTelemetryAppenderInitializer.class));
    }

    @Test
    void beanPresentWhenEnabled() {
        try (MockedStatic<OpenTelemetryAppender> ignored = Mockito.mockStatic(OpenTelemetryAppender.class)) {
            runner.withPropertyValues("management.opentelemetry.enabled=true")
                    .run(ctx -> assertThat(ctx).hasSingleBean(OpenTelemetryAppenderInitializer.class));
        }
    }
}
