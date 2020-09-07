package com.restapi.configuration;

import javax.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class EtagConfiguration {

	@Bean(name = "etagFilter")
	public Filter shallowEtagHeaderFilter() {
		return new ShallowEtagHeaderFilter();
	}

	@Bean
	public FilterRegistrationBean<Filter> ftreg() {
		final FilterRegistrationBean<Filter> fr = new FilterRegistrationBean<>();
		fr.setFilter(shallowEtagHeaderFilter());
		fr.addUrlPatterns("/operation/*");
		fr.setName("etagFilter");
		fr.setOrder(1);
		return fr;
	}
}
