package com.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAddressRequest {

	@NotBlank
	@Size(max = 64)
	private String label;

	@NotBlank
	@Size(max = 255)
	private String line1;

	@Size(max = 255)
	private String line2;

	@NotBlank
	@Size(max = 120)
	private String city;

	@NotBlank
	@Size(max = 32)
	private String postalCode;

	private boolean defaultAddress;
}
