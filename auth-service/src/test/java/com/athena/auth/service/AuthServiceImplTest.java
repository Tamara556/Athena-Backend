package com.athena.auth.service;

import com.athena.auth.domain.Role;
import com.athena.auth.dto.AuthResponse;
import com.athena.auth.dto.LoginRequest;
import com.athena.auth.dto.RefreshRequest;
import com.athena.auth.dto.RegisterRequest;
import com.athena.auth.entity.UserAccount;
import com.athena.auth.repository.UserAccountRepository;
import com.athena.auth.service.impl.AuthServiceImpl;
import com.athena.common.exception.DuplicateResourceException;
import com.athena.common.exception.InvalidCredentialsException;
import com.athena.common.security.JwtService;
import com.athena.common.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private com.athena.auth.messaging.AuthEventPublisher eventPublisher;

    @InjectMocks
    private AuthServiceImpl authService;

    private static UserAccount existingAccount(UUID id, String email, String passwordHash) {
        UserAccount account = new UserAccount();
        account.setId(id);
        account.setFirstName("Ada");
        account.setLastName("Lovelace");
        account.setUsername("ada");
        account.setEmail(email);
        account.setPasswordHash(passwordHash);
        account.addRole(Role.USER);
        return account;
    }

    private void stubTokenIssuing() {
        lenient().when(jwtService.generateAccessToken(any(), anyList())).thenReturn("access-token");
        lenient().when(jwtService.generateRefreshToken(any(), anyList())).thenReturn("refresh-token");
        lenient().when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
    }

    @Test
    void register_persistsAccountAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest("Ada", "Lovelace", "ada", "New.User@Example.com", "Password123!");
        when(userAccountRepository.existsByEmailIgnoreCase("new.user@example.com")).thenReturn(false);
        when(userAccountRepository.existsByUsernameIgnoreCase("ada")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed");
        UUID id = UUID.randomUUID();
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount toSave = invocation.getArgument(0);
            toSave.setId(id);
            return toSave;
        });
        stubTokenIssuing();

        AuthResponse response = authService.register(request);

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        UserAccount saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new.user@example.com");
        assertThat(saved.getUsername()).isEqualTo("ada");
        assertThat(saved.getFirstName()).isEqualTo("Ada");
        assertThat(saved.getLastName()).isEqualTo("Lovelace");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getRoles()).containsExactly(Role.USER);

        assertThat(response.userId()).isEqualTo(id);
        assertThat(response.username()).isEqualTo("ada");
        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.roles()).containsExactly("USER");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userAccountRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("A", "B", "auser", "taken@example.com", "Password123!")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void register_rejectsDuplicateUsername() {
        when(userAccountRepository.existsByEmailIgnoreCase("free@example.com")).thenReturn(false);
        when(userAccountRepository.existsByUsernameIgnoreCase("taken")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("A", "B", "Taken", "free@example.com", "Password123!")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void login_succeedsWithEmail() {
        UUID id = UUID.randomUUID();
        UserAccount account = existingAccount(id, "user@example.com", "stored-hash");
        when(userAccountRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("user@example.com", "user@example.com"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secret123", "stored-hash")).thenReturn(true);
        stubTokenIssuing();

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "secret123"));

        assertThat(response.userId()).isEqualTo(id);
        assertThat(response.username()).isEqualTo("ada");
    }

    @Test
    void login_succeedsWithUsername() {
        UUID id = UUID.randomUUID();
        UserAccount account = existingAccount(id, "user@example.com", "stored-hash");
        when(userAccountRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("ada", "ada"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secret123", "stored-hash")).thenReturn(true);
        stubTokenIssuing();

        AuthResponse response = authService.login(new LoginRequest("Ada", "secret123"));

        assertThat(response.userId()).isEqualTo(id);
    }

    @Test
    void login_failsWithWrongPassword() {
        UserAccount account = existingAccount(UUID.randomUUID(), "user@example.com", "stored-hash");
        when(userAccountRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("user@example.com", "user@example.com"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_failsWhenAccountMissing() {
        when(userAccountRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("ghost@example.com", "ghost@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "whatever1")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_rotatesTokensForValidRefreshToken() {
        UUID id = UUID.randomUUID();
        UserAccount account = existingAccount(id, "user@example.com", "hash");
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtService.parseAndValidate("valid-refresh", TokenType.REFRESH)).thenReturn(claims);
        when(jwtService.extractSubject(claims)).thenReturn(id.toString());
        when(userAccountRepository.findById(id)).thenReturn(Optional.of(account));
        stubTokenIssuing();

        AuthResponse response = authService.refresh(new RefreshRequest("valid-refresh"));

        assertThat(response.userId()).isEqualTo(id);
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void refresh_rejectsInvalidToken() {
        when(jwtService.parseAndValidate(eq("bad"), eq(TokenType.REFRESH)))
                .thenThrow(new JwtException("nope"));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("bad")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
