package com.devsecops.taskflow.service;

import com.devsecops.taskflow.domain.Role;
import com.devsecops.taskflow.domain.User;
import com.devsecops.taskflow.dto.request.LoginRequest;
import com.devsecops.taskflow.dto.request.RegisterRequest;
import com.devsecops.taskflow.exception.DuplicateResourceException;
import com.devsecops.taskflow.exception.InvalidCredentialException;
import com.devsecops.taskflow.repository.UserRepository;
import com.devsecops.taskflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthService authService;
    private BCryptPasswordEncoder encoder;
    private User savedUser;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(userRepository, encoder, authenticationManager, jwtService);

        savedUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@taskflow.local")
                .fullName("Alice")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();
    }

    private RegisterRequest registerRequest() {
        return new RegisterRequest("alice", "alice@taskflow.local", "Password123!", "Alice", null);
    }

    @Test
    void registerEncodesPasswordAndIssuesToken() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@taskflow.local")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            savedUser = invocation.getArgument(0);
            return savedUser;
        });
        when(jwtService.issue(any(User.class)))
                .thenReturn(new JwtService.Token("jwt-token", Instant.now().plusSeconds(60)));

        var response = authService.register(registerRequest());

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().role()).isEqualTo(Role.ROLE_USER.authority());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        assertThat(persisted.getEmail()).isEqualTo("alice@taskflow.local");
        assertThat(persisted.getPassword()).isNotEqualTo("Password123!");
        assertThat(encoder.matches("Password123!", persisted.getPassword())).isTrue();
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@taskflow.local")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginRejectsBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong-password")))
                .isInstanceOf(InvalidCredentialException.class);
    }

    @Test
    void loginIssuesTokenOnSuccess() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(savedUser));
        when(jwtService.issue(savedUser))
                .thenReturn(new JwtService.Token("jwt-token", Instant.now().plusSeconds(60)));

        var response = authService.login(new LoginRequest("alice", "Password123!"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().username()).isEqualTo("alice");
    }
}