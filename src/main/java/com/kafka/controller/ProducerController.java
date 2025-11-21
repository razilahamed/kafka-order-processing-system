package com.example.kafka.controller;

import com.example.kafka.config.KafkaPropertiesFactory;
import com.example.kafka.config.PropertiesLoader;
import com.example.kafka.config.SchemaLoader;
import com.example.kafka.service.OrderProducerService;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerController.class);

    public static void main(String[] args) throws Exception {
        Properties properties = PropertiesLoader.load("application.properties");
        Schema schema = SchemaLoader.load("order.avsc");

        String topic = properties.getProperty("demo.topic.orders", "orders");
        int messagesPerSecond = Integer.parseInt(properties.getProperty("demo.producer.messages.per.second", "1"));

        Properties producerProps = KafkaPropertiesFactory.producer(properties);
        OrderProducerService producerService = new OrderProducerService(producerProps, schema, topic, messagesPerSecond);

        LOGGER.info("Bootstrapping producer controller for topic {}", topic);
        producerService.startProducing();
    }
}
