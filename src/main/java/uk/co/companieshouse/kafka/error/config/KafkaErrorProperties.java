package uk.co.companieshouse.kafka.error.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "kafka.error")
public class KafkaErrorProperties {

    @NotBlank
    String retryTopic;

    @NotBlank
    String errorTopic;

    private String consumerGroupId = "kafka-error";

    private String errorBootstrapAddress = "localhost:9092";

    private String retryBootstrapAddress = errorBootstrapAddress; // Use the same address unless overridden

    private Long startOffset;

    private Long endOffset;

    private Integer partition = 0;

    public String getRetryTopic() {
        return retryTopic;
    }

    public void setRetryTopic(String retryTopic) {
        this.retryTopic = retryTopic;
    }

    public String getErrorTopic() {
        return errorTopic;
    }

    public void setErrorTopic(String errorTopic) {
        this.errorTopic = errorTopic;
    }

    public String getConsumerGroupId() {
        return consumerGroupId;
    }

    public void setConsumerGroupId(String consumerGroupId) {
        this.consumerGroupId = consumerGroupId;
    }

    public String getErrorBootstrapAddress() {
        return errorBootstrapAddress;
    }

    public void setErrorBootstrapAddress(String errorBootstrapAddress) {
        this.errorBootstrapAddress = errorBootstrapAddress;
    }

    public String getRetryBootstrapAddress() {
        return retryBootstrapAddress;
    }

    public void setRetryBootstrapAddress(String retryBootstrapAddress) {
        this.retryBootstrapAddress = retryBootstrapAddress;
    }

    public Long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(Long startOffset) {
        this.startOffset = startOffset;
    }

    public Long getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(Long endOffset) {
        this.endOffset = endOffset;
    }

    public Integer getPartition() {
        return partition;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }
}
