package com.logitrack.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PaymentRoutesConfig {
    @Bean
    RouteLocator paymentRoutes(RouteLocatorBuilder builder) {
        return builder.routes().route("payment-service-payments", route -> route
                .path("/api/v1/payments/**").uri("lb://PAYMENT-SERVICE")).build();
    }
}
