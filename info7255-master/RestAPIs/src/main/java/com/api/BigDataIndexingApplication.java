package com.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.api.util.JsonUtil;

@SpringBootApplication
public class BigDataIndexingApplication {

	public static void main(String[] args) {
		
		JsonUtil.loadSchema();
		SpringApplication.run(BigDataIndexingApplication.class, args);
	}
}
