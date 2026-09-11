package com.user_service.controller;

import com.user_service.dto.CreateAddressRequest;
import com.user_service.dto.CreateProfileRequest;
import com.user_service.dto.DeliveryAddressResponse;
import com.user_service.dto.UpdateAddressRequest;
import com.user_service.dto.UpdateProfileRequest;
import com.user_service.dto.UserProfileResponse;
import com.user_service.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// User Profile
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

	private final UserProfileService userProfileService;

	@PostMapping("/profiles")
	public ResponseEntity<UserProfileResponse> createProfile(@Valid @RequestBody CreateProfileRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userProfileService.createProfile(request));
	}

	@GetMapping("/profiles/{id}")
	public ResponseEntity<UserProfileResponse> getProfile(@PathVariable Long id) {
		return ResponseEntity.ok(userProfileService.getProfileById(id));
	}

	@GetMapping("/profiles/by-auth/{authUserId}")
	public ResponseEntity<UserProfileResponse> getProfileByAuth(@PathVariable Long authUserId) {
		return ResponseEntity.ok(userProfileService.getProfileByAuthUserId(authUserId));
	}

	@PutMapping("/profiles/{id}")
	public ResponseEntity<UserProfileResponse> updateProfile(
			@PathVariable Long id,
			@Valid @RequestBody UpdateProfileRequest request) {
		return ResponseEntity.ok(userProfileService.updateProfile(id, request));
	}

	@GetMapping("/profiles/{profileId}/addresses")
	public ResponseEntity<List<DeliveryAddressResponse>> listAddresses(@PathVariable Long profileId) {
		return ResponseEntity.ok(userProfileService.listAddresses(profileId));
	}

	@PostMapping("/profiles/{profileId}/addresses")
	public ResponseEntity<DeliveryAddressResponse> addAddress(
			@PathVariable Long profileId,
			@Valid @RequestBody CreateAddressRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userProfileService.addAddress(profileId, request));
	}

	@PutMapping("/addresses/{addressId}")
	public ResponseEntity<DeliveryAddressResponse> updateAddress(
			@PathVariable Long addressId,
			@Valid @RequestBody UpdateAddressRequest request) {
		return ResponseEntity.ok(userProfileService.updateAddress(addressId, request));
	}

	@DeleteMapping("/addresses/{addressId}")
	public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId) {
		userProfileService.deleteAddress(addressId);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/addresses/{addressId}/default")
	public ResponseEntity<DeliveryAddressResponse> markDefault(@PathVariable Long addressId) {
		return ResponseEntity.ok(userProfileService.markDefaultAddress(addressId));
	}
}
