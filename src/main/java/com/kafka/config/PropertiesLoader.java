package com.example.kafka.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PropertiesLoader {

    private PropertiesLoader() {
    }

    public static Properties load(String resourceName) throws IOException {
        String externalPath = System.getenv("APP_CONFIG_PATH");
        if (externalPath != null && !externalPath.isBlank()) {
            try (InputStream fileInput = new FileInputStream(externalPath)) {
                Properties props = new Properties();
                props.load(fileInput);
                return props;
            }
        }

        try (InputStream input = PropertiesLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException(resourceName + " not found");
            }
            Properties props = new Properties();
            props.load(input);
            return props;
        }
    }
}
