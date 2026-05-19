package com.moriba.skultem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SkultemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkultemApplication.class, args);
	}

}
