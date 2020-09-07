package com.restapi;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.restapi.json.JsonUtil;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		JsonUtil.loadSchema();
		ApplicationContext ctx = SpringApplication.run(Application.class, args);

		System.out.println("This is the First Demo fot INFO 7255: Advanced Bigdata Application/Indexing");

		String[] beanNames = ctx.getBeanDefinitionNames();
		Arrays.sort(beanNames);
		for (String beanName : beanNames) {
			System.out.println(beanName);
		}
	}
	
}
