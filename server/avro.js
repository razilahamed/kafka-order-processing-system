const fs = require('fs');
const path = require('path');
const avro = require('avsc');

const schemaPath = path.join(__dirname, 'order.avsc');
const schema = JSON.parse(fs.readFileSync(schemaPath, 'utf8'));
const OrderType = avro.Type.forSchema(schema);

function serializeOrder(order) {
  return OrderType.toBuffer(order); // Buffer
}

function deserializeOrder(buffer) {
  return OrderType.fromBuffer(buffer); // JS object
}

module.exports = {
  serializeOrder,
  deserializeOrder
};
