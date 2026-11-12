package uk.co.companieshouse.kafka.error.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;

class ReplayIT extends KafkaIntegrationSupport {

    @Test
    void forwardsExactBytesAndAddsReplayTimestamp() throws Exception {
        byte[] payload = {0, 1, 2, (byte) 0xff, 10, 0};
        seed(0, "outside-range");
        send(0, payload);
        startApplication(1L, 1L, 0);
        long beforeReplay = System.currentTimeMillis();
        startReplay();

        var records = read(1);
        assertThat(records.getFirst().value()).containsExactly(payload);
        var header = records.getFirst().headers().lastHeader(DltConsumer.REPLAY_HEADER_MILLIS);
        assertThat(header).isNotNull();
        long timestamp = Long.parseLong(new String(header.value(), StandardCharsets.UTF_8));
        assertThat(timestamp).isBetween(beforeReplay, System.currentTimeMillis());
        awaitPaused(0);
        awaitCommitted(0, 2);
        assertNoMoreRecords();
    }

    @Test
    void replaysOnlyTheInclusiveOffsetRangeInOrder() throws Exception {
        seed(0, "zero", "one", "two", "three", "four");
        startApplication(1L, 3L, 0);
        startReplay();

        assertPayloads(read(3), "one", "two", "three");
        awaitPaused(0);
        awaitCommitted(0, 4);
        seed(0, "five");
        assertNoMoreRecords();
    }

    @Test
    void snapshotsDefaultEndOffsetOnFirstRecordAndExcludesLaterMessages() throws Exception {
        CountDownLatch secondRecord = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        startApplication(null, null, 0, WebApplicationType.NONE, offset -> {
            if (offset == 1) {
                secondRecord.countDown();
                awaitRelease(release);
            }
        });
        // The application starts with an empty topic; the snapshot is taken when replay begins.
        seed(0, "zero", "one", "two");
        try {
            startReplay();
            assertThat(secondRecord.await(30, TimeUnit.SECONDS)).isTrue();
            assertPayloads(read(1), "zero");
            seed(0, "after-snapshot");
        } finally {
            release.countDown();
        }

        assertPayloads(read(2), "one", "two");
        awaitPaused(0);
        awaitCommitted(0, 3);
        assertNoMoreRecords();
    }

    @Test
    void consumesOnlyTheConfiguredPartition() throws Exception {
        seed(0, "unselected-partition");
        seed(1, "selected-partition");
        startApplication(0L, 0L, 1);
        startReplay();

        assertPayloads(read(1), "selected-partition");
        awaitPaused(1);
        awaitCommitted(1, 1);
        assertNoMoreRecords();
    }

    @Test
    void waitsForFirstMessageOnAnEmptyTopic() throws Exception {
        startApplication(null, null, 0);
        startReplay();
        assertNoMoreRecords();
        assertThat(listener().isRunning()).isTrue();

        seed(0, "arrived-after-startup");
        assertPayloads(read(1), "arrived-after-startup");
        awaitPaused(0);
        awaitCommitted(0, 1);
        assertNoMoreRecords();
    }

}
