const { Kafka } = require('kafkajs');
const express = require('express');
const cors = require('cors');
const { deserializeOrder } = require('./avro');

const kafka = new Kafka({
  clientId: 'order-consumer',
  brokers: ['localhost:9092']
});

const ordersTopic = 'orders';
const dlqTopic = 'orders-dlq';

const consumer = kafka.consumer({ groupId: 'order-consumer-group' });
const dlqProducer = kafka.producer();

class TemporaryProcessingError extends Error {}

class RunningAverage {
  constructor() {
    this.totalSum = 0;
    this.count = 0;
    this.perProduct = {}; // { product: { sum, count } }
  }

  update(product, price) {
    this.totalSum += price;
    this.count += 1;

    if (!this.perProduct[product]) {
      this.perProduct[product] = { sum: price, count: 1 };
    } else {
      this.perProduct[product].sum += price;
      this.perProduct[product].count += 1;
    }
  }

  overallAverage() {
    return this.count === 0 ? 0 : this.totalSum / this.count;
  }

  productAverage(product) {
    const p = this.perProduct[product];
    if (!p || p.count === 0) return 0;
    return p.sum / p.count;
  }

  snapshot() {
    const perProductStats = {};
    for (const [product, { sum, count }] of Object.entries(this.perProduct)) {
      perProductStats[product] = {
        count,
        averagePrice: count === 0 ? 0 : sum / count
      };
    }

    return {
      totalCount: this.count,
      overallAveragePrice: this.overallAverage(),
      perProduct: perProductStats
    };
  }

  log() {
    console.log(
      `[AVG] count=${this.count}, overallAverage=${this.overallAverage().toFixed(
        2
      )}`
    );
    for (const product of Object.keys(this.perProduct)) {
      console.log(
        `      ${product}: count=${this.perProduct[product].count}, avg=${this.productAverage(
          product
        ).toFixed(2)}`
      );
    }
  }
}

const averages = new RunningAverage();
const dlqEvents = []; // for UI – last few DLQ events

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function sendToDlq(rawValue, reason, order) {
  console.warn(`[DLQ] reason="${reason}"`);

  dlqEvents.unshift({
    orderId: order && order.orderId ? order.orderId : null,
    reason,
    timestamp: new Date().toISOString()
  });

  // keep only last 20
  if (dlqEvents.length > 20) {
    dlqEvents.pop();
  }

  await dlqProducer.send({
    topic: dlqTopic,
    messages: [
      {
        value: rawValue,
        headers: {
          errorReason: Buffer.from(reason)
        }
      }
    ]
  });
}

async function processOrder(order) {
  // Simulate a transient error about 10% of the time
  if (Math.random() < 0.1) {
    throw new TemporaryProcessingError('Simulated transient processing error');
  }

  const price = Number(order.price);
  const product = order.product;

  averages.update(product, price);

  console.log(
    `[OK] orderId=${order.orderId} product=${product} price=${price.toFixed(2)}`
  );
  averages.log();
}

const MAX_RETRIES = 3;
const RETRY_BACKOFF_MS = 1000;

async function startConsumer() {
  await consumer.connect();
  await dlqProducer.connect();

  await consumer.subscribe({ topic: ordersTopic, fromBeginning: true });

  await consumer.run({
    autoCommit: true,
    eachMessage: async ({ topic, partition, message }) => {
      const raw = message.value;
      let order;

      try {
        order = deserializeOrder(raw);
      } catch (err) {
        console.error('[DESERIALIZATION ERROR]', err.message);
        await sendToDlq(raw, `Deserialization error: ${err.message}`);
        return;
      }

      let attempt = 0;
      while (true) {
        try {
          await processOrder(order);
          // success – autoCommit takes care of offset
          break;
        } catch (err) {
          if (err instanceof TemporaryProcessingError) {
            attempt += 1;
            if (attempt <= MAX_RETRIES) {
              const backoff = RETRY_BACKOFF_MS * attempt;
              console.warn(
                `[RETRY] orderId=${order.orderId}, attempt ${attempt}/${MAX_RETRIES}, backoff ${backoff}ms`
              );
              await delay(backoff);
              continue;
            }
            console.error(
              `[DLQ] Max retries exceeded for orderId=${order.orderId}`
            );
            await sendToDlq(raw, `Max retries exceeded: ${err.message}`, order);
            break;
          } else {
            console.error('[PERMANENT ERROR]', err);
            await sendToDlq(raw, `Permanent error: ${err.message}`, order);
            break;
          }
        }
      }
    }
  });

  console.log('Kafka consumer running...');
}

function startApiServer() {
  const app = express();
  app.use(cors());

  app.get('/metrics', (req, res) => {
    res.json(averages.snapshot());
  });

  app.get('/dlq', (req, res) => {
    res.json(dlqEvents);
  });

  const port = 4000;
  app.listen(port, () => {
    console.log(`Metrics API listening on http://localhost:${port}`);
  });
}

startConsumer().catch(console.error);
startApiServer();
