package com.example.kafka.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public class RetryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryService.class);

    private final int maxAttempts;
    private final long initialBackoffMs;
    private final String retryTopic;
    private final DlqService dlqService;
    private final KafkaProducer<String, Object> retryProducer;

    public RetryService(int maxAttempts, long initialBackoffMs, String retryTopic,
                        DlqService dlqService, KafkaProducer<String, Object> retryProducer) {
        this.maxAttempts = maxAttempts;
        this.initialBackoffMs = initialBackoffMs;
        this.retryTopic = retryTopic;
        this.dlqService = dlqService;
        this.retryProducer = retryProducer;
    }

    public void handleFailure(ConsumerRecord<String, Object> record, Exception exception) {
        int attempt = currentAttempt(record) + 1;
        if (attempt <= maxAttempts) {
            long backoff = (long) (initialBackoffMs * Math.pow(2, attempt - 1));
            LOGGER.warn("Retrying record with key {} (attempt {} of {}), waiting {} ms", record.key(), attempt, maxAttempts, backoff, exception);
            sleep(backoff);
            ProducerRecord<String, Object> retryRecord = new ProducerRecord<>(retryTopic, record.key(), record.value());
            retryRecord.headers().add(new RecordHeader("retries", String.valueOf(attempt).getBytes(StandardCharsets.UTF_8)));
            retryProducer.send(retryRecord, (metadata, sendEx) -> {
                if (sendEx != null) {
                    LOGGER.error("Failed to send record to retry topic", sendEx);
                } else {
                    LOGGER.info("Sent record to retry topic {} partition {} offset {}", metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        } else {
            LOGGER.error("Exceeded max retries for key {}. Sending to DLQ", record.key(), exception);
            dlqService.send(record);
        }
    }

    private int currentAttempt(ConsumerRecord<String, Object> record) {
        Optional<Header> header = Optional.ofNullable(record.headers().lastHeader("retries"));
        if (header.isEmpty()) {
            return 0;
        }
        try {
            String value = new String(header.get().value(), StandardCharsets.UTF_8);
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            LOGGER.warn("Invalid retries header value", ex);
            return 0;
        }
    }

    private void sleep(long durationMs) {
        try {
            Thread.sleep(Duration.ofMillis(durationMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
