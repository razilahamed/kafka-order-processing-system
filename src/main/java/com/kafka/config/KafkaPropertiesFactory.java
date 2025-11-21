package com.example.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public final class KafkaPropertiesFactory {

    private KafkaPropertiesFactory() {
    }

    public static Properties producer(Properties base) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, base.getProperty("demo.kafka.bootstrap.servers"));
        props.put("schema.registry.url", base.getProperty("demo.schema.registry.url"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, base.getProperty("demo.producer.acks", "all"));
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.parseInt(base.getProperty("demo.producer.retries", "5")));
        props.put(ProducerConfig.LINGER_MS_CONFIG, Integer.parseInt(base.getProperty("demo.producer.linger.ms", "20")));
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, Integer.parseInt(base.getProperty("demo.producer.batch.size", "16384")));
        props.put(ProducerConfig.CLIENT_ID_CONFIG, base.getProperty("demo.producer.clientId", "order-producer"));
        return props;
    }

    public static Properties consumer(Properties base) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, base.getProperty("demo.kafka.bootstrap.servers"));
        props.put("schema.registry.url", base.getProperty("demo.schema.registry.url"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, base.getProperty("demo.consumer.group.id", "order-consumer-group"));
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, base.getProperty("demo.consumer.client.id", "order-consumer"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        props.put("specific.avro.reader", false);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.parseInt(base.getProperty("demo.consumer.max.poll.records", "50")));
        return props;
    }

    public static Properties retryProducer(Properties producerProps, Properties base) {
        Properties retryProps = new Properties();
        retryProps.putAll(producerProps);
        retryProps.put(ProducerConfig.CLIENT_ID_CONFIG, base.getProperty("demo.consumer.client.id", "order-retry-producer"));
        return retryProps;
    }
}
