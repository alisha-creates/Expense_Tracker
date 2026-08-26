package com.example.NexSpend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NexSpendApplication {

	public static void main(String[] args) {

		SpringApplication.run(NexSpendApplication.class, args);
	}
}
