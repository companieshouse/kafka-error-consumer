package uk.co.companieshouse.kafka.error.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;

class ReplayRestartIT extends KafkaIntegrationSupport {

    @Test
    void resumesFromCommittedOffsetWhenStartIsOmitted() throws Exception {
        seed(0, "zero", "one", "two");
        startApplication(null, 0L, 0);
        startReplay();
        assertPayloads(read(1), "zero");
        awaitPaused(0);
        awaitCommitted(0, 1);
        closeApplication();

        startApplication(null, 2L, 0);
        startReplay();
        assertPayloads(read(2), "one", "two");
        awaitPaused(0);
        awaitCommitted(0, 3);
        assertNoMoreRecords();
    }

    @Test
    void explicitStartOffsetOverridesCommittedProgressForIntentionalReplay() throws Exception {
        seed(0, "zero", "one");
        startApplication(0L, 1L, 0);
        startReplay();
        assertPayloads(read(2), "zero", "one");
        awaitPaused(0);
        awaitCommitted(0, 2);
        closeApplication();

        startApplication(0L, 0L, 0);
        startReplay();
        assertPayloads(read(1), "zero");
        awaitPaused(0);
        awaitCommitted(0, 1);
        assertNoMoreRecords();
    }

    @Test
    void recoversFromBrokerInterruptionWithoutLosingTheInFlightRecord() throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        seed(0, "survives-broker-interruption");
        startApplication(0L, 0L, 0, WebApplicationType.NONE, offset -> {
            received.countDown();
            awaitRelease(release);
        });
        startReplay();
        try {
            assertThat(received.await(30, TimeUnit.SECONDS)).isTrue();
            KAFKA.getDockerClient().pauseContainerCmd(KAFKA.getContainerId()).exec();
            try {
                release.countDown();
                assertNoMoreRecords();
                assertThat(application.isActive()).isTrue();
            } finally {
                KAFKA.getDockerClient().unpauseContainerCmd(KAFKA.getContainerId()).exec();
            }
        } finally {
            release.countDown();
        }

        assertPayloads(read(1), "survives-broker-interruption");
        awaitPaused(0);
        awaitCommitted(0, 1);
        assertNoMoreRecords();
    }
}
