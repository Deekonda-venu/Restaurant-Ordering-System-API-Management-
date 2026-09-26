package com.example.Delivary_Serivce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class DelivarySerivceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DelivarySerivceApplication.class, args);
	}

}
