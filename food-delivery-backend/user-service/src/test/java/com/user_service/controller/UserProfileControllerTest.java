package com.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.CreateProfileRequest;
import com.user_service.dto.DeliveryAddressResponse;
import com.user_service.dto.UserProfileResponse;
import com.user_service.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private UserProfileService userProfileService;

	@Test
	void createProfile_returns201() throws Exception {
		CreateProfileRequest body = CreateProfileRequest.builder()
				.authUserId(10L)
				.displayName("Alex")
				.phone("555")
				.build();
		UserProfileResponse response = UserProfileResponse.builder()
				.id(1L)
				.authUserId(10L)
				.displayName("Alex")
				.phone("555")
				.contactEmail(null)
				.createdAt(Instant.parse("2026-01-01T00:00:00Z"))
				.updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
				.addresses(List.of())
				.build();
		when(userProfileService.createProfile(any(CreateProfileRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/users/profiles")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.authUserId").value(10));

		verify(userProfileService).createProfile(any(CreateProfileRequest.class));
	}

	@Test
	void getProfile_returnsOk() throws Exception {
		UserProfileResponse response = UserProfileResponse.builder()
				.id(2L)
				.authUserId(20L)
				.displayName("Sam")
				.phone(null)
				.contactEmail(null)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.addresses(List.of())
				.build();
		when(userProfileService.getProfileById(2L)).thenReturn(response);

		mockMvc.perform(get("/api/users/profiles/2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Sam"));

		verify(userProfileService).getProfileById(2L);
	}

	@Test
	void getProfileByAuth_returnsOk() throws Exception {
		UserProfileResponse response = UserProfileResponse.builder()
				.id(3L)
				.authUserId(99L)
				.displayName("Pat")
				.phone(null)
				.contactEmail(null)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.addresses(List.of())
				.build();
		when(userProfileService.getProfileByAuthUserId(99L)).thenReturn(response);

		mockMvc.perform(get("/api/users/profiles/by-auth/99"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(3));

		verify(userProfileService).getProfileByAuthUserId(99L);
	}

	@Test
	void addAddress_returns201() throws Exception {
		String json = """
				{"label":"HOME","line1":"1 Main","line2":null,"city":"NYC","postalCode":"10001","defaultAddress":true}
				""";
		DeliveryAddressResponse addr = DeliveryAddressResponse.builder()
				.id(5L)
				.label("HOME")
				.line1("1 Main")
				.line2(null)
				.city("NYC")
				.postalCode("10001")
				.defaultAddress(true)
				.build();
		when(userProfileService.addAddress(eq(1L), any())).thenReturn(addr);

		mockMvc.perform(post("/api/users/profiles/1/addresses")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(5));

		verify(userProfileService).addAddress(eq(1L), any());
	}

	@Test
	void updateProfile_returnsOk() throws Exception {
		String json = "{\"displayName\":\"New Name\",\"phone\":null,\"contactEmail\":null}";
		UserProfileResponse response = UserProfileResponse.builder()
				.id(1L)
				.authUserId(1L)
				.displayName("New Name")
				.phone(null)
				.contactEmail(null)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.addresses(List.of())
				.build();
		when(userProfileService.updateProfile(eq(1L), any())).thenReturn(response);

		mockMvc.perform(put("/api/users/profiles/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("New Name"));

		verify(userProfileService).updateProfile(eq(1L), any());
	}
}
