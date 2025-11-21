package com.example.kafka.model;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

public class Order {
    private final String orderId;
    private final String product;
    private final double price;

    public Order(String orderId, String product, double price) {
        this.orderId = orderId;
        this.product = product;
        this.price = price;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProduct() {
        return product;
    }

    public double getPrice() {
        return price;
    }

    public GenericRecord toGenericRecord(Schema schema) {
        GenericRecord record = new GenericData.Record(schema);
        record.put("orderId", orderId);
        record.put("product", product);
        record.put("price", price);
        return record;
    }

    public static Order fromGenericRecord(GenericRecord record) {
        String orderId = record.get("orderId").toString();
        String product = record.get("product").toString();
        double price = Double.parseDouble(record.get("price").toString());
        return new Order(orderId, product, price);
    }
}
