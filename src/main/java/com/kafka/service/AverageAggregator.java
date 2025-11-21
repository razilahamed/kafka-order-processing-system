package com.example.kafka.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a running average price per product.
 */
public class AverageAggregator {
    private final Map<String, ProductStats> statsByProduct = new ConcurrentHashMap<>();

    public synchronized double updateAverage(String product, double price) {
        ProductStats stats = statsByProduct.computeIfAbsent(product, key -> new ProductStats());
        stats.total += price;
        stats.count += 1;
        return stats.total / stats.count;
    }

    private static class ProductStats {
        private double total;
        private long count;
    }
}
