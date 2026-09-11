package com.notification_service.notification_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileChangedPayload {

	public enum ChangeType {
		CREATED,
		UPDATED
	}

	private ChangeType type;
	private Long profileId;
	private Long authUserId;
	private Instant occurredAt;
}
