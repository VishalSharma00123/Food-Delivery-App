package com.user_service.service;

import com.fooddelivery.rbac.RbacSupport;
import com.fooddelivery.rbac.SecurityRoleUtils;
import com.user_service.dto.CreateAddressRequest;
import com.user_service.dto.CreateProfileRequest;
import com.user_service.dto.DeliveryAddressResponse;
import com.user_service.dto.UpdateAddressRequest;
import com.user_service.dto.UpdateProfileRequest;
import com.user_service.dto.UserProfileResponse;
import com.user_service.entity.DeliveryAddress;
import com.user_service.entity.UserProfile;
import com.user_service.exception.AddressNotFoundException;
import com.user_service.exception.BadRequestException;
import com.user_service.exception.DuplicateProfileException;
import com.user_service.exception.ProfileNotFoundException;
import com.user_service.kafka.UserProfileChangedPayload;
import com.user_service.kafka.UserProfileEventPublisher;
import com.user_service.mapper.UserProfileMapper;
import com.user_service.repository.DeliveryAddressRepository;
import com.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

	private final UserProfileRepository userProfileRepository;
	private final DeliveryAddressRepository deliveryAddressRepository;
	private final UserProfileEventPublisher userProfileEventPublisher;

	@Override
	@Transactional
	public UserProfileResponse createProfile(CreateProfileRequest request) {
		if (!SecurityRoleUtils.isAdmin()) {
			Long uid = RbacSupport.requireUserId();
			if (!uid.equals(request.getAuthUserId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create profile for another user");
			}
		}
		if (userProfileRepository.existsByAuthUserId(request.getAuthUserId())) {
			throw new DuplicateProfileException("Profile already exists for authUserId=" + request.getAuthUserId());
		}
		UserProfile profile = UserProfile.builder()
				.authUserId(request.getAuthUserId())
				.displayName(request.getDisplayName().trim())
				.phone(trimToNull(request.getPhone()))
				.contactEmail(trimToNull(request.getContactEmail()))
				.build();
		UserProfile saved = userProfileRepository.save(profile);
		userProfileEventPublisher.publish(UserProfileChangedPayload.ChangeType.CREATED, saved.getId(), saved.getAuthUserId());
		return UserProfileMapper.toResponse(userProfileRepository.findByIdWithAddresses(saved.getId()).orElse(saved));
	}

	@Override
	@Transactional(readOnly = true)
	public UserProfileResponse getProfileById(Long id) {
		UserProfile profile = userProfileRepository.findByIdWithAddresses(id)
				.orElseThrow(() -> new ProfileNotFoundException("Profile not found: id=" + id));
		assertProfileAccess(profile);
		return UserProfileMapper.toResponse(profile);
	}

	@Override
	@Transactional(readOnly = true)
	public UserProfileResponse getProfileByAuthUserId(Long authUserId) {
		RbacSupport.assertSelfOrAdmin(authUserId, "profile");
		UserProfile profile = userProfileRepository.findByAuthUserIdWithAddresses(authUserId)
				.orElseThrow(() -> new ProfileNotFoundException("Profile not found: authUserId=" + authUserId));
		return UserProfileMapper.toResponse(profile);
	}

	@Override
	@Transactional
	public UserProfileResponse updateProfile(Long id, UpdateProfileRequest request) {
		if (request.getDisplayName() == null && request.getPhone() == null && request.getContactEmail() == null) {
			throw new BadRequestException("At least one field must be provided to update");
		}
		UserProfile profile = userProfileRepository.findById(id)
				.orElseThrow(() -> new ProfileNotFoundException("Profile not found: id=" + id));
		assertProfileAccess(profile);
		if (request.getDisplayName() != null) {
			String dn = request.getDisplayName().trim();
			if (dn.isEmpty()) {
				throw new BadRequestException("displayName cannot be blank");
			}
			profile.setDisplayName(dn);
		}
		if (request.getPhone() != null) {
			profile.setPhone(trimToNull(request.getPhone()));
		}
		if (request.getContactEmail() != null) {
			profile.setContactEmail(trimToNull(request.getContactEmail()));
		}
		userProfileRepository.save(profile);
		userProfileEventPublisher.publish(UserProfileChangedPayload.ChangeType.UPDATED, profile.getId(), profile.getAuthUserId());
		return UserProfileMapper.toResponse(userProfileRepository.findByIdWithAddresses(id).orElse(profile));
	}

	@Override
	@Transactional(readOnly = true)
	public List<DeliveryAddressResponse> listAddresses(Long profileId) {
		UserProfile profile = userProfileRepository.findById(profileId)
				.orElseThrow(() -> new ProfileNotFoundException("Profile not found: id=" + profileId));
		assertProfileAccess(profile);
		return deliveryAddressRepository.findByProfile_IdOrderByDefaultAddressDescIdAsc(profileId).stream()
				.map(UserProfileMapper::toAddressResponse)
				.toList();
	}

	@Override
	@Transactional
	public DeliveryAddressResponse addAddress(Long profileId, CreateAddressRequest request) {
		UserProfile profile = userProfileRepository.findById(profileId)
				.orElseThrow(() -> new ProfileNotFoundException("Profile not found: id=" + profileId));
		assertProfileAccess(profile);
		if (request.isDefaultAddress()) {
			clearDefaults(profileId);
		}
		DeliveryAddress address = DeliveryAddress.builder()
				.profile(profile)
				.label(request.getLabel().trim())
				.line1(request.getLine1().trim())
				.line2(trimToNull(request.getLine2()))
				.city(request.getCity().trim())
				.postalCode(request.getPostalCode().trim())
				.defaultAddress(request.isDefaultAddress())
				.build();
		DeliveryAddress saved = deliveryAddressRepository.save(address);
		userProfileEventPublisher.publish(UserProfileChangedPayload.ChangeType.UPDATED, profile.getId(), profile.getAuthUserId());
		return UserProfileMapper.toAddressResponse(saved);
	}

	@Override
	@Transactional
	public DeliveryAddressResponse updateAddress(Long addressId, UpdateAddressRequest request) {
		if (request.getLabel() == null && request.getLine1() == null && request.getLine2() == null
				&& request.getCity() == null && request.getPostalCode() == null) {
			throw new BadRequestException("At least one field must be provided to update");
		}
		DeliveryAddress address = deliveryAddressRepository.findById(addressId)
				.orElseThrow(() -> new AddressNotFoundException("Address not found: id=" + addressId));
		assertProfileAccess(address.getProfile());
		if (request.getLabel() != null) {
			String v = request.getLabel().trim();
			if (v.isEmpty()) {
				throw new BadRequestException("label cannot be blank");
			}
			address.setLabel(v);
		}
		if (request.getLine1() != null) {
			String v = request.getLine1().trim();
			if (v.isEmpty()) {
				throw new BadRequestException("line1 cannot be blank");
			}
			address.setLine1(v);
		}
		if (request.getLine2() != null) {
			address.setLine2(trimToNull(request.getLine2()));
		}
		if (request.getCity() != null) {
			String v = request.getCity().trim();
			if (v.isEmpty()) {
				throw new BadRequestException("city cannot be blank");
			}
			address.setCity(v);
		}
		if (request.getPostalCode() != null) {
			String v = request.getPostalCode().trim();
			if (v.isEmpty()) {
				throw new BadRequestException("postalCode cannot be blank");
			}
			address.setPostalCode(v);
		}
		deliveryAddressRepository.save(address);
		UserProfile profile = address.getProfile();
		userProfileEventPublisher.publish(UserProfileChangedPayload.ChangeType.UPDATED, profile.getId(), profile.getAuthUserId());
		return UserProfileMapper.toAddressResponse(address);
	}

	@Override
	@Transactional
	public void deleteAddress(Long addressId) {
		DeliveryAddress address = deliveryAddressRepository.findById(addressId)
				.orElseThrow(() -> new AddressNotFoundException("Address not found: id=" + addressId));
		assertProfileAccess(address.getProfile());
		UserProfile profile = address.getProfile();
		Long profileId = profile.getId();
		boolean wasDefault = address.isDefaultAddress();
		deliveryAddressRepository.delete(address);
		if (wasDefault) {
			List<DeliveryAddress> remaining = deliveryAddressRepository.findByProfile_IdOrderByDefaultAddressDescIdAsc(profileId);
			if (!remaining.isEmpty()) {
				DeliveryAddress first = remaining.get(0);
				first.setDefaultAddress(true);
				deliveryAddressRepository.save(first);
			}
		}
		userProfileEventPublisher.publish(UserProfileChangedPayload.ChangeType.UPDATED, profileId, profile.getAuthUserId());
	}

	@Override
	@Transactional
	public DeliveryAddressResponse markDefaultAddress(Long addressId) {
		DeliveryAddress address = deliveryAddressRepository.findById(addressId)
				.orElseThrow(() -> new AddressNotFoundException("Address not found: id=" + addressId));
		assertProfileAccess(address.getProfile());
		UserProfile profile = address.getProfile();
		clearDefaults(profile.getId());
		address.setDefaultAddress(true);
		deliveryAddressRepository.save(address);
		userProfileEventPublisher.publish(UserProfileChangedPayload.ChangeType.UPDATED, profile.getId(), profile.getAuthUserId());
		return UserProfileMapper.toAddressResponse(address);
	}

	private void assertProfileAccess(UserProfile profile) {
		if (SecurityRoleUtils.isAdmin()) {
			return;
		}
		Long uid = RbacSupport.requireUserId();
		if (!uid.equals(profile.getAuthUserId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this profile");
		}
	}

	private void clearDefaults(Long profileId) {
		List<DeliveryAddress> list = deliveryAddressRepository.findByProfile_Id(profileId);
		for (DeliveryAddress d : list) {
			d.setDefaultAddress(false);
		}
		deliveryAddressRepository.saveAll(list);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String t = value.trim();
		return t.isEmpty() ? null : t;
	}
}
