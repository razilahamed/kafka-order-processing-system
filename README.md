# Kafka Orders Event-Driven Demo

Event-driven orders processing system built for a big-data / streaming assignment:

- **Backend**: Node.js, KafkaJS, Avro
  - Produces random `Order` events to Kafka.
  - Consumes events, maintains **running averages**, and implements **retry + DLQ**.
  - Exposes a small **REST API** for metrics and DLQ events.
- **Frontend**: Vite + React + Tailwind CSS
  - Real-time dashboard showing totals, averages, and recent DLQ entries.

---

## 1. Architecture Overview

### Data Flow

1. **Producer (`backend/producer.js`)**

   - Generates random `Order` events.
   - Serialises each order with **Avro** using `order.avsc`.
   - Publishes messages to Kafka topic **`orders`**.

2. **Consumer + API (`backend/consumer-api.js`)**

   - Subscribes to **`orders`**.
   - Deserialises Avro payloads.
   - Applies business logic with **simulated transient failures**:
     - Retries on `TemporaryProcessingError` up to `MAX_RETRIES`.
     - After exhaustion or permanent failure, forwards message to **DLQ topic `orders-dlq`**.
   - Maintains **running averages**:
     - Overall count and average price.
     - Per-product counts and averages.
   - Exposes an HTTP API:
     - `GET /metrics` → running averages snapshot.
     - `GET /dlq` → recent DLQ events (in-memory list).

3. **Frontend (`frontend/`)**
   - Polls the backend every second:
     - `GET http://localhost:4000/metrics`
     - `GET http://localhost:4000/dlq`
   - Renders:
     - Total orders processed.
     - Overall average price.
     - Per-product statistics table.
     - Recent DLQ events table (time, orderId, reason).

---

## 2. Tech Stack

**Backend**

- Node.js
- KafkaJS
- Avro (`avsc`)
- Express
- CORS

**Frontend**

- Vite
- React
- Tailwind CSS (via `@tailwindcss/vite` plugin)

**Infrastructure**

- Apache Kafka (local installation **or** Docker Compose)
- Topic names:
  - `orders`
  - `orders-dlq`

---

## 3. Project Structure

```text
kafka-orders-event-driven-demo/
  docker-compose.yml        # Optional: Zookeeper + Kafka via Docker
  backend/
    package.json
    order.avsc              # Avro schema for Order events
    avro.js                 # Avro (de)serialisation helpers
    producer.js             # Kafka producer (random orders)
    consumer-api.js         # Kafka consumer + retry + DLQ + REST API
  frontend/
    package.json
    vite.config.js          # Vite + Tailwind plugin config
    index.html
    src/
      main.jsx              # React entrypoint
      App.jsx               # Dashboard UI
      index.css             # Tailwind entry (imports `@import "tailwindcss";`)

```

## How It Works

- Producer generates random `Order` events every 1–2 seconds and publishes them to the `orders` topic (Avro-encoded).
- Kafka stores the events durably and streams them to the consumer in the `order-consumer-group`.
- Consumer deserialises each message, applies business logic, and updates running averages (overall and per product) in memory.
- On transient errors, the consumer retries processing up to 3 times with a backoff delay before giving up.
- Messages that still fail (or have deserialisation/permanent errors) are sent to the `orders-dlq` topic with an `errorReason` header.
- The same consumer process exposes a REST API (`/metrics`, `/dlq`) which the React + Tailwind dashboard polls every second to render live stats and recent DLQ events.

---

## Quick start (light)

- Ensure Kafka is running (local or via Docker Compose). Create topics: `orders`, `orders-dlq`.
- Start the backend services (from project root or `backend/`):

```powershell
# install and run producer + consumer (example)
cd backend
npm install
npm run producer   # starts producing random orders
npm run consumer   # runs the consumer + REST API on port 4000
```

- Start the frontend dashboard (from project root):

```powershell
cd client
npm install
npm run dev
# then open http://localhost:5173 (or the port shown by Vite)
```
