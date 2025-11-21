package com.example.kafka.service;

import com.example.kafka.model.Order;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class OrderConsumerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderConsumerService.class);

    private final KafkaConsumer<String, Object> consumer;
    private final RetryService retryService;
    private final AverageAggregator averageAggregator;
    private final String ordersTopic;
    private final String retryTopic;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public OrderConsumerService(KafkaConsumer<String, Object> consumer,
                                RetryService retryService,
                                AverageAggregator averageAggregator,
                                String ordersTopic,
                                String retryTopic) {
        this.consumer = consumer;
        this.retryService = retryService;
        this.averageAggregator = averageAggregator;
        this.ordersTopic = ordersTopic;
        this.retryTopic = retryTopic;
    }

    public void start() {
        consumer.subscribe(Arrays.asList(ordersTopic, retryTopic));
        LOGGER.info("Consumer started. Subscribed to {} and {}", ordersTopic, retryTopic);
        while (running.get()) {
            try {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, Object> record : records) {
                    try {
                        processRecord(record);
                        consumer.commitAsync();
                    } catch (Exception ex) {
                        retryService.handleFailure(record, ex);
                    }
                }
            } catch (WakeupException e) {
                if (running.get()) {
                    throw e;
                }
            }
        }
    }

    public void shutdown() {
        running.set(false);
        consumer.wakeup();
    }

    private void processRecord(ConsumerRecord<String, Object> record) {
        Object value = record.value();
        if (!(value instanceof GenericRecord genericRecord)) {
            throw new IllegalArgumentException("Unexpected record type: " + value);
        }
        Order order = Order.fromGenericRecord(genericRecord);
        double average = averageAggregator.updateAverage(order.getProduct(), order.getPrice());
        LOGGER.info("Consumed key {} product {} price {} -> running average {}", record.key(), order.getProduct(), order.getPrice(), average);
    }
}
