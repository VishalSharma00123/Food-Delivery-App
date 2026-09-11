package com.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileRequest {

	@NotNull
	@Positive
	private Long authUserId;

	@NotBlank
	@Size(max = 160)
	private String displayName;

	@Size(max = 32)
	private String phone;

	@Size(max = 255)
	private String contactEmail;
}
