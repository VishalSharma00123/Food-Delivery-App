package com.auth_service.auth_service.controller;

import com.auth_service.auth_service.dto.AuthResponse;
import com.auth_service.auth_service.dto.LoginRequest;
import com.auth_service.auth_service.dto.RegisterRequest;
import com.auth_service.auth_service.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void register_returnsAuthResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("pass");
        request.setRole("CUSTOMER");

        AuthResponse response = AuthResponse.builder()
                .token("tok")
                .userId(1L)
                .email("new@example.com")
                .roles(List.of("CUSTOMER"))
                .build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);/// Whenever controller calls authService.register(...)
        ///→ return THIS response

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void login_returnsAuthResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("new@example.com");
        request.setPassword("pass");

        AuthResponse response = AuthResponse.builder()
                .token("jwt")
                .userId(2L)
                .email("new@example.com")
                .roles(List.of("CUSTOMER"))
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"));
    }
}
