const { Kafka } = require('kafkajs');
const { serializeOrder } = require('./avro');

const kafka = new Kafka({
  clientId: 'order-producer',
  brokers: ['localhost:9092'] // adjust if needed
});

const topic = 'orders';

const products = ['Item1', 'Item2', 'Item3', 'Item4'];

function randomOrderId(len = 6) {
  const digits = '0123456789';
  let out = '';
  for (let i = 0; i < len; i += 1) {
    out += digits[Math.floor(Math.random() * digits.length)];
  }
  return out;
}

function buildRandomOrder() {
  return {
    orderId: randomOrderId(),
    product: products[Math.floor(Math.random() * products.length)],
    price: Number((10 + Math.random() * 490).toFixed(2))
  };
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function run() {
  const producer = kafka.producer();
  await producer.connect();
  console.log(`Producer connected. Sending orders to topic "${topic}"...`);

  try {
    while (true) {
      const order = buildRandomOrder();
      const payload = serializeOrder(order);

      await producer.send({
        topic,
        messages: [{ value: payload }]
      });

      console.log('Produced', order);
      await delay(1000); // 1 order per second
    }
  } catch (err) {
    console.error('Producer error:', err);
  } finally {
    await producer.disconnect();
  }
}

run().catch(console.error);
