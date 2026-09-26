package com.example.Notification_Serivce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class NotificationSerivceApplication {

	public static void main(String[] args) {
		SpringApplication.run(
				NotificationSerivceApplication.class,
				args
		);
	}
}