package com.ai.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiLibraryApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiLibraryApplication.class, args);
    }
}
