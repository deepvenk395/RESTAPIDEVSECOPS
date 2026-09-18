package com.devsecops.taskflow.config;

import com.devsecops.taskflow.web.RequestLoggingFilter;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> registration =
                new FilterRegistrationBean<>(new RequestLoggingFilter());
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
        registration.addUrlPatterns("/*");
        return registration;
    }
}