package com.user_service.service;

import com.user_service.dto.CreateAddressRequest;
import com.user_service.dto.CreateProfileRequest;
import com.user_service.dto.UpdateProfileRequest;
import com.user_service.entity.DeliveryAddress;
import com.user_service.entity.UserProfile;
import com.user_service.exception.DuplicateProfileException;
import com.user_service.exception.ProfileNotFoundException;
import com.user_service.kafka.UserProfileChangedPayload;
import com.user_service.kafka.UserProfileEventPublisher;
import com.user_service.repository.DeliveryAddressRepository;
import com.user_service.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	private void asAuthenticatedUser(long userId) {
		UsernamePasswordAuthenticationToken auth =
				new UsernamePasswordAuthenticationToken("test", null, List.of());
		auth.setDetails(userId);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Mock
	private UserProfileRepository userProfileRepository;

	@Mock
	private DeliveryAddressRepository deliveryAddressRepository;

	@Mock
	private UserProfileEventPublisher userProfileEventPublisher;

	@InjectMocks
	private UserProfileServiceImpl userProfileService;

	@Test
	void createProfile_duplicateAuthUserId_throws() {
		asAuthenticatedUser(1L);
		when(userProfileRepository.existsByAuthUserId(1L)).thenReturn(true);
		CreateProfileRequest req = CreateProfileRequest.builder()
				.authUserId(1L)
				.displayName("A")
				.build();

		assertThatThrownBy(() -> userProfileService.createProfile(req))
				.isInstanceOf(DuplicateProfileException.class);

		verify(userProfileRepository, never()).save(any());
		verify(userProfileEventPublisher, never()).publish(
				org.mockito.ArgumentMatchers.any(UserProfileChangedPayload.ChangeType.class),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void createProfile_savesAndPublishesCreated() {
		asAuthenticatedUser(2L);
		when(userProfileRepository.existsByAuthUserId(2L)).thenReturn(false);
		UserProfile saved = UserProfile.builder()
				.id(10L)
				.authUserId(2L)
				.displayName("Bob")
				.phone(null)
				.contactEmail(null)
				.build();
		when(userProfileRepository.save(any(UserProfile.class))).thenReturn(saved);
		when(userProfileRepository.findByIdWithAddresses(10L)).thenReturn(Optional.of(saved));

		CreateProfileRequest req = CreateProfileRequest.builder()
				.authUserId(2L)
				.displayName("Bob")
				.build();

		assertThat(userProfileService.createProfile(req).getId()).isEqualTo(10L);

		ArgumentCaptor<UserProfileChangedPayload.ChangeType> typeCaptor =
				ArgumentCaptor.forClass(UserProfileChangedPayload.ChangeType.class);
		verify(userProfileEventPublisher).publish(typeCaptor.capture(), eq(10L), eq(2L));
		assertThat(typeCaptor.getValue()).isEqualTo(UserProfileChangedPayload.ChangeType.CREATED);
	}

	@Test
	void getProfileById_missing_throws() {
		when(userProfileRepository.findByIdWithAddresses(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userProfileService.getProfileById(99L))
				.isInstanceOf(ProfileNotFoundException.class);
	}

	@Test
	void addAddress_clearsDefaultsWhenNewIsDefault() {
		asAuthenticatedUser(5L);
		UserProfile profile = UserProfile.builder()
				.id(1L)
				.authUserId(5L)
				.displayName("X")
				.build();
		when(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
		when(deliveryAddressRepository.save(any(DeliveryAddress.class))).thenAnswer(inv -> inv.getArgument(0));

		CreateAddressRequest req = CreateAddressRequest.builder()
				.label("HOME")
				.line1("10 St")
				.city("Town")
				.postalCode("12345")
				.defaultAddress(true)
				.build();

		userProfileService.addAddress(1L, req);

		verify(deliveryAddressRepository).findByProfile_Id(1L);
		verify(deliveryAddressRepository).saveAll(any());
		verify(deliveryAddressRepository).save(any(DeliveryAddress.class));
		verify(userProfileEventPublisher).publish(UserProfileChangedPayload.ChangeType.UPDATED, 1L, 5L);
	}

	@Test
	void listAddresses_returnsMappedList() {
		asAuthenticatedUser(7L);
		UserProfile profile = UserProfile.builder()
				.id(1L)
				.authUserId(7L)
				.displayName("P")
				.build();
		when(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
		DeliveryAddress a = DeliveryAddress.builder()
				.id(3L)
				.label("W")
				.line1("L1")
				.city("C")
				.postalCode("P")
				.defaultAddress(false)
				.build();
		when(deliveryAddressRepository.findByProfile_IdOrderByDefaultAddressDescIdAsc(1L)).thenReturn(List.of(a));

		var list = userProfileService.listAddresses(1L);
		assertThat(list).hasSize(1);
		assertThat(list.get(0).getId()).isEqualTo(3L);
	}
}
