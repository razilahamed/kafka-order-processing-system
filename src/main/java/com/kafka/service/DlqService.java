package com.example.kafka.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DlqService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DlqService.class);

    private final KafkaProducer<String, Object> producer;
    private final String dlqTopic;

    public DlqService(KafkaProducer<String, Object> producer, String dlqTopic) {
        this.producer = producer;
        this.dlqTopic = dlqTopic;
    }

    public void send(ConsumerRecord<String, Object> record) {
        ProducerRecord<String, Object> dlqRecord = new ProducerRecord<>(dlqTopic, record.key(), record.value());
        dlqRecord.headers().add(record.headers());
        producer.send(dlqRecord, (metadata, exception) -> {
            if (exception != null) {
                LOGGER.error("Failed to send record to DLQ", exception);
            } else {
                LOGGER.info("Sent record to DLQ topic {} partition {} offset {}", metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }
}
