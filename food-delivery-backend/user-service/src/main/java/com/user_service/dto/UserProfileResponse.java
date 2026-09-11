package com.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

	private Long id;
	private Long authUserId;
	private String displayName;
	private String phone;
	private String contactEmail;
	private Instant createdAt;
	private Instant updatedAt;
	private List<DeliveryAddressResponse> addresses;
}
