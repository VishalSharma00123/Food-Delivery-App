package com.user_service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UserProfileEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(UserProfileEventPublisher.class);

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final String topic;

	public UserProfileEventPublisher(
			KafkaTemplate<String, Object> kafkaTemplate,
			@Value("${kafka.topics.user-profile-changed}") String topic) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
	}

	public void publish(UserProfileChangedPayload.ChangeType type, Long profileId, Long authUserId) {
		Object payload = UserProfileChangedPayload.builder()
				.type(type)
				.profileId(profileId)
				.authUserId(authUserId)
				.occurredAt(Instant.now())
				.build();
		try {
			kafkaTemplate.send(topic, String.valueOf(authUserId), payload).whenComplete((r, ex) -> {
				if (ex != null) {
					log.warn("Failed to publish user profile event for authUserId={}: {}", authUserId, ex.getMessage());
				}
			});
		} catch (Exception e) {
			log.warn("Kafka publish threw for authUserId={}: {}", authUserId, e.getMessage());
		}
	}
}
