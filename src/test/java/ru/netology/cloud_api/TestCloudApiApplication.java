package ru.netology.cloud_api;

import org.springframework.boot.SpringApplication;

public class TestCloudApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(CloudApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
