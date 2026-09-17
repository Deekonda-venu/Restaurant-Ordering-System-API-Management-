package com.example.Resturant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ResturantApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResturantApplication.class, args);
	}

}
