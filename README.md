# Kafka Order Processing System

Lightweight Java + Avro demo: produces `Order` events to `orders`, retries transient failures to `orders-retry`, forwards permanent failures to `orders-dlq`, and maintains a running average of order prices per product.

Requirements

- Java 17+
- Maven 3.9+
- Kafka broker (Schema Registry optional)

Quick start

- Build: `mvn -q -DskipTests package`
- Run producer: `mvn -q exec:java -Dexec.mainClass="com.example.kafka.controller.ProducerController"`
- Run consumer: `mvn -q exec:java -Dexec.mainClass="com.example.kafka.controller.ConsumerController"`

Config

- Defaults: `src/main/resources/application.properties`
- Override by setting `APP_CONFIG_PATH` when starting the app.

Docker

- Build: `docker build -t kafka-order-system .`
- Run (consumer): `docker run --rm -e MAIN_CLASS=com.example.kafka.controller.ConsumerController kafka-order-system`

Topics

- `orders`, `orders-retry`, `orders-dlq`

See source under `src/main/java/com/example/kafka` for controllers, services, and Avro schema in `src/main/resources`.
