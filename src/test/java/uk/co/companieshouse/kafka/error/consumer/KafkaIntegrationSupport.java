package uk.co.companieshouse.kafka.error.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;
import uk.co.companieshouse.kafka.error.KafkaErrorApplication;

abstract class KafkaIntegrationSupport {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
            DockerImageName.parse(System.getProperty("kafka.test.image", "confluentinc/cp-kafka:7.8.0")));

    String errorTopic;
    String retryTopic;
    String groupId;
    Admin admin;
    ConfigurableApplicationContext application;
    private KafkaProducer<String, byte[]> producer;
    private KafkaConsumer<String, byte[]> observer;

    @BeforeAll
    static void startBroker() {
        KAFKA.start();
    }

    @AfterAll
    static void stopBroker() {
        KAFKA.stop();
    }

    @BeforeEach
    void createTopics() throws Exception {
        String id = UUID.randomUUID().toString();
        errorTopic = "error-" + id;
        retryTopic = "retry-" + id;
        groupId = "replay-" + id;
        admin = Admin.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()));
        admin.createTopics(List.of(new NewTopic(errorTopic, 2, (short) 1),
                new NewTopic(retryTopic, 1, (short) 1))).all().get(30, TimeUnit.SECONDS);
        producer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.ACKS_CONFIG, "all"),
                new StringSerializer(), new ByteArraySerializer());
        observer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false),
                new StringDeserializer(), new ByteArrayDeserializer());
        observer.assign(List.of(new TopicPartition(retryTopic, 0)));
        observer.seek(new TopicPartition(retryTopic, 0), 0);
    }

    @AfterEach
    void cleanUp() throws Exception {
        try {
            closeApplication();
        } finally {
            try {
                if (observer != null) {
                    observer.close(Duration.ofSeconds(5));
                }
                if (producer != null) {
                    producer.close(Duration.ofSeconds(5));
                }
            } finally {
                if (admin != null) {
                    try {
                        admin.deleteTopics(List.of(errorTopic, retryTopic)).all()
                                .get(30, TimeUnit.SECONDS);
                    } finally {
                        admin.close(Duration.ofSeconds(5));
                    }
                }
            }
        }
    }

    void startApplication(Long start, Long end, int partition) {
        startApplication(start, end, partition, WebApplicationType.NONE, offset -> { });
    }

    void startApplication(Long start, Long end, int partition, WebApplicationType web,
            LongConsumer beforeRecord) {
        Map<String, String> properties = new HashMap<>(Map.of(
                "kafka.error.bootstrapAddress", KAFKA.getBootstrapServers(),
                "kafka.error.errorTopic", errorTopic,
                "kafka.error.retryTopic", retryTopic,
                "kafka.error.consumerGroupId", groupId,
                "kafka.error.partition", Integer.toString(partition),
                "logger.namespace", "integration-test",
                "otel.sdk.disabled", "true",
                "server.port", "0",
                "logging.level.root", "WARN",
                "spring.main.banner-mode", "off"));
        if (start != null) {
            properties.put("kafka.error.startOffset", start.toString());
        }
        if (end != null) {
            properties.put("kafka.error.endOffset", end.toString());
        }
        String[] arguments = properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
        application = new SpringApplicationBuilder(KafkaErrorApplication.class)
                .web(web)
                .initializers(context -> context.getBeanFactory().addBeanPostProcessor(
                        new BeanPostProcessor() {
                            @Override
                            public Object postProcessAfterInitialization(Object bean, String name) {
                                if ("kafkaListenerContainerFactory".equals(name)
                                        && bean instanceof ConcurrentKafkaListenerContainerFactory<?, ?> factory) {
                                    // Seed records before starting the real listener, without replacing its factory.
                                    factory.setAutoStartup(false);
                                    factory.setRecordInterceptor((record, consumer) -> {
                                        beforeRecord.accept(record.offset());
                                        return record;
                                    });
                                }
                                return bean;
                            }
                        }))
                .run(arguments);
    }

    void startReplay() {
        listener().start();
        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(listener().getAssignedPartitions()).isNotEmpty());
    }

    MessageListenerContainer listener() {
        return application.getBean(KafkaListenerEndpointRegistry.class)
                .getListenerContainer(DltConsumer.CONSUMER_ID);
    }

    void closeApplication() {
        if (application != null) {
            application.close();
            application = null;
        }
    }

    void send(int partition, byte[] payload) throws Exception {
        producer.send(new ProducerRecord<>(errorTopic, partition, "test-key", payload))
                .get(30, TimeUnit.SECONDS);
    }

    void seed(int partition, String... messages) throws Exception {
        for (String message : messages) {
            send(partition, message.getBytes(StandardCharsets.UTF_8));
        }
    }

    List<ConsumerRecord<String, byte[]>> read(int count) {
        List<ConsumerRecord<String, byte[]>> records = new ArrayList<>();
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (records.size() < count && System.nanoTime() < deadline) {
            observer.poll(Duration.ofMillis(200)).forEach(records::add);
        }
        assertThat(records).hasSize(count);
        return records;
    }

    void assertNoMoreRecords() {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        do {
            assertThat(observer.poll(Duration.ofMillis(200)).isEmpty()).isTrue();
        } while (System.nanoTime() < deadline);
    }

    void awaitPaused(int partition) {
        await().atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(listener().isPartitionPaused(new TopicPartition(errorTopic, partition)))
                    .isTrue();
            assertThat(listener().isRunning()).isTrue();
        });
    }

    void awaitCommitted(int partition, long expectedOffset) {
        await().atMost(TIMEOUT).untilAsserted(() -> {
            var offsets = admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata()
                    .get(5, TimeUnit.SECONDS);
            var committed = offsets.get(new TopicPartition(errorTopic, partition));
            assertThat(committed).isNotNull();
            assertThat(committed.offset()).isEqualTo(expectedOffset);
        });
    }

    void assertPayloads(List<ConsumerRecord<String, byte[]>> records, String... expected) {
        assertThat(records).extracting(record ->
                new String(record.value(), StandardCharsets.UTF_8)).containsExactly(expected);
    }

    static void awaitRelease(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release the replay listener");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Replay listener interrupted", exception);
        }
    }
}
