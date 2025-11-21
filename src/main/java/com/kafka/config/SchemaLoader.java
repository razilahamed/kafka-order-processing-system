package com.example.kafka.config;

import org.apache.avro.Schema;

import java.io.IOException;
import java.io.InputStream;

public final class SchemaLoader {

    private SchemaLoader() {
    }

    public static Schema load(String schemaResource) throws IOException {
        try (InputStream input = SchemaLoader.class.getClassLoader().getResourceAsStream(schemaResource)) {
            if (input == null) {
                throw new IllegalStateException(schemaResource + " not found");
            }
            return new Schema.Parser().parse(input);
        }
    }
}
