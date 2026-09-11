package com.user_service.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

	@Size(max = 160)
	private String displayName;

	@Size(max = 32)
	private String phone;

	@Size(max = 255)
	private String contactEmail;
}
