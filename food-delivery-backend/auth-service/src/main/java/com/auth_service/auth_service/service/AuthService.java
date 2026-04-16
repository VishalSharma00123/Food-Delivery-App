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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserCredentialRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserCredentialRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists.");
        }

        Role role = roleRepository.findByName(request.getRole().toUpperCase())
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(request.getRole().toUpperCase());
                    return roleRepository.save(newRole);
                });

        UserCredential user = UserCredential.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .roles(Collections.singleton(role))
                .build();

        user = userRepository.save(user);

        List<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        String token = jwtService.generateToken(user.getEmail(), user.getId(), roleNames);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roleNames)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserCredential user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        String token = jwtService.generateToken(user.getEmail(), user.getId(), roleNames);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roleNames)
                .build();
    }
}