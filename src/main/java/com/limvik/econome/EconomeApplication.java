package com.limvik.econome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EconomeApplication {

	public static void main(String[] args) {
		SpringApplication.run(EconomeApplication.class, args);
	}

}
