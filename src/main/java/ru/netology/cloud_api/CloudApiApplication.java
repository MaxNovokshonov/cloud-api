package ru.netology.cloud_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ru.netology.cloud_api.config.AppProperties;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "ru.netology.cloud_api.repository")
@EntityScan(basePackages = "ru.netology.cloud_api.domain")
@EnableConfigurationProperties(AppProperties.class)
public class CloudApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudApiApplication.class, args);
    }
}
