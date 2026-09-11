package com.user_service.service;

import com.user_service.dto.CreateAddressRequest;
import com.user_service.dto.CreateProfileRequest;
import com.user_service.dto.DeliveryAddressResponse;
import com.user_service.dto.UpdateAddressRequest;
import com.user_service.dto.UpdateProfileRequest;
import com.user_service.dto.UserProfileResponse;

import java.util.List;

public interface UserProfileService {

	UserProfileResponse createProfile(CreateProfileRequest request);

	UserProfileResponse getProfileById(Long id);

	UserProfileResponse getProfileByAuthUserId(Long authUserId);

	UserProfileResponse updateProfile(Long id, UpdateProfileRequest request);

	List<DeliveryAddressResponse> listAddresses(Long profileId);

	DeliveryAddressResponse addAddress(Long profileId, CreateAddressRequest request);

	DeliveryAddressResponse updateAddress(Long addressId, UpdateAddressRequest request);

	void deleteAddress(Long addressId);

	DeliveryAddressResponse markDefaultAddress(Long addressId);
}
