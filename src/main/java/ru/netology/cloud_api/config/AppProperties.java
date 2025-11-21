package ru.netology.cloud_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors = new Cors();

    public Cors getCors() {
        return cors;
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:8081");

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }
}


