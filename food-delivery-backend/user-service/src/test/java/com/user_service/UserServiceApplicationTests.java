package com.user_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
@Import(UserServiceApplicationTests.KafkaTestConfig.class)
class UserServiceApplicationTests {

	@TestConfiguration
	static class KafkaTestConfig {

		@Bean
		@Primary
		KafkaTemplate<String, Object> kafkaTemplate() {
			return mock(KafkaTemplate.class);
		}
	}

	@Test
	void contextLoads() {
	}
}
