package com.auth_service.auth_service.service;

import com.auth_service.auth_service.dto.AuthResponse;
import com.auth_service.auth_service.dto.LoginRequest;
import com.auth_service.auth_service.dto.RegisterRequest;
import com.auth_service.auth_service.entity.Role;
import com.auth_service.auth_service.entity.UserCredential;
import com.auth_service.auth_service.exception.UserAlreadyExistsException;
import com.auth_service.auth_service.repository.RoleRepository;
import com.auth_service.auth_service.repository.UserCredentialRepository;
import com.auth_service.auth_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserCredentialRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("user@example.com");
        registerRequest.setPassword("secret");
        registerRequest.setRole("customer");

        customerRole = Role.builder().id(1L).name("CUSTOMER").build();
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest));
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    void register_createsUserAndReturnsToken_whenEmailIsNewAndRoleExists() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(UserCredential.class))).thenAnswer(invocation -> {
            UserCredential u = invocation.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(jwtService.generateToken(eq("user@example.com"), eq(42L), eq(List.of("CUSTOMER"))))
                .thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(42L, response.getUserId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals(List.of("CUSTOMER"), response.getRoles());
        verify(userRepository).save(any(UserCredential.class));
    }

    @Test
    void register_createsRole_whenMissing() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role r = invocation.getArgument(0);
            r.setId(9L);
            return r;
        });
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(UserCredential.class))).thenAnswer(invocation -> {
            UserCredential u = invocation.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(jwtService.generateToken(eq("user@example.com"), eq(7L), eq(List.of("CUSTOMER"))))
                .thenReturn("jwt");

        AuthResponse response = authService.register(registerRequest);

        assertEquals("jwt", response.getToken());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void login_returnsToken_afterAuthentication() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("owner@example.com");
        loginRequest.setPassword("pwd");

        Set<Role> roles = new HashSet<>();
        roles.add(Role.builder().id(2L).name("RESTAURANT_OWNER").build());
        UserCredential user = UserCredential.builder()
                .id(99L)
                .email("owner@example.com")
                .password("hash")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .roles(roles)
                .build();

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(eq("owner@example.com"), eq(99L), eq(List.of("RESTAURANT_OWNER"))))
                .thenReturn("login-jwt");

        AuthResponse response = authService.login(loginRequest);

        assertEquals("login-jwt", response.getToken());
        assertEquals(99L, response.getUserId());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
