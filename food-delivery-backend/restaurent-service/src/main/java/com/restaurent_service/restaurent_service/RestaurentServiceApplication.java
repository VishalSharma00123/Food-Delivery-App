package com.restaurent_service.restaurent_service;

import com.fooddelivery.rbac.autoconfigure.RbacAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RbacAutoConfiguration.class)
public class RestaurentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurentServiceApplication.class, args);
    }
}
