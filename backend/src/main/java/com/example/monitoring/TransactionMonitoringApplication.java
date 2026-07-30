package com.example.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TransactionMonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionMonitoringApplication.class, args);
    }
}

