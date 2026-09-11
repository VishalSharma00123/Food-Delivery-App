package com.user_service.mapper;

import com.user_service.dto.DeliveryAddressResponse;
import com.user_service.dto.UserProfileResponse;
import com.user_service.entity.DeliveryAddress;
import com.user_service.entity.UserProfile;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class UserProfileMapper {

	private UserProfileMapper() {
	}

	public static UserProfileResponse toResponse(UserProfile profile) {
		List<DeliveryAddress> raw = profile.getAddresses();
		if (raw == null) {
			raw = Collections.emptyList();
		}
		List<DeliveryAddressResponse> addresses = raw.stream()
				.sorted(Comparator.comparing(DeliveryAddress::isDefaultAddress).reversed()
						.thenComparing(DeliveryAddress::getId))
				.map(UserProfileMapper::toAddressResponse)
				.toList();
		return UserProfileResponse.builder()
				.id(profile.getId())
				.authUserId(profile.getAuthUserId())
				.displayName(profile.getDisplayName())
				.phone(profile.getPhone())
				.contactEmail(profile.getContactEmail())
				.createdAt(profile.getCreatedAt())
				.updatedAt(profile.getUpdatedAt())
				.addresses(addresses)
				.build();
	}

	public static DeliveryAddressResponse toAddressResponse(DeliveryAddress address) {
		return DeliveryAddressResponse.builder()
				.id(address.getId())
				.label(address.getLabel())
				.line1(address.getLine1())
				.line2(address.getLine2())
				.city(address.getCity())
				.postalCode(address.getPostalCode())
				.defaultAddress(address.isDefaultAddress())
				.build();
	}
}
