package com.example.kafka.service;

import com.example.kafka.model.Order;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class OrderProducerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProducerService.class);

    private final Properties producerProps;
    private final Schema schema;
    private final String topic;
    private final int messagesPerSecond;
    private final Random random = new Random();
    private final String[] products = new String[]{"keyboard", "mouse", "monitor", "laptop", "dock"};

    public OrderProducerService(Properties producerProps, Schema schema, String topic, int messagesPerSecond) {
        this.producerProps = producerProps;
        this.schema = schema;
        this.topic = topic;
        this.messagesPerSecond = messagesPerSecond;
    }

    public void startProducing() throws InterruptedException {
        try (KafkaProducer<String, GenericRecord> producer = new KafkaProducer<>(producerProps)) {
            LOGGER.info("Starting producer sending to topic {} at {} msg/sec", topic, messagesPerSecond);
            while (true) {
                Order order = randomOrder();
                GenericRecord record = order.toGenericRecord(schema);

                ProducerRecord<String, GenericRecord> producerRecord = new ProducerRecord<>(topic, order.getOrderId(), record);
                producer.send(producerRecord, (metadata, exception) -> {
                    if (exception != null) {
                        LOGGER.error("Failed to send message", exception);
                    } else {
                        LOGGER.info("Produced record to topic {} partition {} offset {}", metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });

                Thread.sleep(Duration.ofSeconds(1).toMillis() / messagesPerSecond);
            }
        }
    }

    private Order randomOrder() {
        String product = products[random.nextInt(products.length)];
        double price = 50 + (250 * random.nextFloat());
        return new Order(UUID.randomUUID().toString(), product, price);
    }
}
