package com.example.Kitchen_Serivce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class KitchenSerivceApplication {

	public static void main(String[] args) {
		SpringApplication.run(KitchenSerivceApplication.class, args);
	}

}
