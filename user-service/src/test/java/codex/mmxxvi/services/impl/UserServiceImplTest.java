package codex.mmxxvi.services.impl;

import codex.mmxxvi.dto.request.CreateUserRequest;
import codex.mmxxvi.dto.request.LoginRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.UpdateUserRequest;
import codex.mmxxvi.dto.response.JwtResponse;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.UserResponse;
import codex.mmxxvi.entity.User;
import codex.mmxxvi.exception.AppExceptions;
import codex.mmxxvi.repository.UserRepository;
import codex.mmxxvi.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMIN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerUserEncodesPasswordAndForcesPublicRole() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("password123")
                .role(1)
                .build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(USER_ID);
            return user;
        });

        UserResponse response = userService.registerUser(request).block();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(USER_ID);
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRole()).isZero();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getRole()).isZero();
    }

    @Test
    void registerUserRejectsDuplicateEmail() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request).block())
                .isInstanceOf(AppExceptions.ConflictException.class);
    }

    @Test
    void loginReturnsTokensForValidCredentials() {
        User user = user("alice", "alice@example.com", "encoded-password", 0);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        JwtResponse response = userService.login(LoginRequest.builder()
                .email("alice@example.com")
                .password("password123")
                .build()).block();

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void loginRejectsInvalidPassword() {
        User user = user("alice", "alice@example.com", "encoded-password", 0);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(LoginRequest.builder()
                .email("alice@example.com")
                .password("wrong")
                .build()).block())
                .isInstanceOf(AppExceptions.UnauthorizedException.class);
    }

    @Test
    void getAllUsersRequiresAdminAndReturnsPage() {
        User user = user("alice", "alice@example.com", "encoded-password", 0);
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        PageResponse<UserResponse> response = withAuth(
                userService.getAllUsers(new PageRequestDto()),
                ADMIN_ID,
                1,
                "user.read"
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getAllUsersRejectsUnsupportedSortField() {
        PageRequestDto request = new PageRequestDto();
        request.setSortBy("password");

        assertThatThrownBy(() -> withAuth(userService.getAllUsers(request), ADMIN_ID, 1, "user.read"))
                .isInstanceOf(AppExceptions.BadRequestException.class);
    }

    @Test
    void updateAllowsOwnerToEditOwnBasicFields() {
        User existingUser = user("alice", "alice@example.com", "old-password", 0);
        UpdateUserRequest request = new UpdateUserRequest("alice2", "alice2@example.com", "password123", null);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername("alice2")).thenReturn(false);
        when(userRepository.existsByEmail("alice2@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("new-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = withAuth(userService.update(USER_ID.toString(), request), USER_ID, 0, "user.self");

        assertThat(response.getUsername()).isEqualTo("alice2");
        assertThat(response.getEmail()).isEqualTo("alice2@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("new-password");
    }

    @Test
    void deleteRejectsInvalidUserId() {
        assertThatThrownBy(() -> userService.delete("not-a-uuid").block())
                .isInstanceOf(AppExceptions.BadRequestException.class);
    }

    private User user(String username, String email, String password, int role) {
        return User.builder()
                .id(USER_ID)
                .username(username)
                .email(email)
                .password(password)
                .role(role)
                .build();
    }

    private <T> T withAuth(Mono<T> mono, UUID userId, int role, String scopes) {
        return mono.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication(userId, role, scopes)))
                .block();
    }

    private Authentication authentication(UUID userId, int role, String scopes) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claims(claims -> claims.putAll(Map.of(
                        "tenantId", "public",
                        "userId", userId.toString(),
                        "role", role,
                        "scope", scopes,
                        "type", "access"
                )))
                .build();
        return new JwtAuthenticationToken(jwt, List.of());
    }
}
