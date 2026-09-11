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
public class UpdateAddressRequest {

	@Size(max = 64)
	private String label;

	@Size(max = 255)
	private String line1;

	@Size(max = 255)
	private String line2;

	@Size(max = 120)
	private String city;

	@Size(max = 32)
	private String postalCode;
}
