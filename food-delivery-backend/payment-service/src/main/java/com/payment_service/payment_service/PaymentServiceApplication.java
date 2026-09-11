package com.payment_service.payment_service;

import com.fooddelivery.rbac.autoconfigure.RbacAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@Import(RbacAutoConfiguration.class)
@EnableKafka
public class PaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

}
