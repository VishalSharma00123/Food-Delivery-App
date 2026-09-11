package com.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddressResponse {

	private Long id;
	private String label;
	private String line1;
	private String line2;
	private String city;
	private String postalCode;
	private boolean defaultAddress;
}
