package com.thoughtworks.problem1application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Problem1Application {

	public static void main(String[] args) {
		SpringApplication.run(Problem1Application.class, args);
	}
}