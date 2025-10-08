package com.gym.service.gymmanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class GymManagementServiceApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(GymManagementServiceApplication.class)
                .properties("spring.config.additional-location=optional:classpath:application-secrets.properties")
                .run(args);
    }
}
