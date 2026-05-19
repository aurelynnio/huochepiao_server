package codex.mmxxvi.services.impl;

import codex.mmxxvi.dto.request.CreateUserRequest;
import codex.mmxxvi.dto.request.ForgotPasswordRequest;
import codex.mmxxvi.dto.request.LoginRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.ResetPasswordRequest;
import codex.mmxxvi.dto.request.UpdateUserRequest;
import codex.mmxxvi.dto.response.JwtResponse;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.PasswordResetResponse;
import codex.mmxxvi.dto.response.UserResponse;
import codex.mmxxvi.entity.PasswordResetToken;
import codex.mmxxvi.entity.User;
import codex.mmxxvi.exception.AppExceptions;
import codex.mmxxvi.repository.PasswordResetTokenRepository;
import codex.mmxxvi.repository.UserRepository;
import codex.mmxxvi.services.JwtService;
import codex.mmxxvi.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "username", "email", "role");
    private static final int ROLE_ADMIN = 1;
    private static final int PASSWORD_RESET_EXPIRY_MINUTES = 15;
    private static final String RESET_TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final boolean passwordResetPreviewEnabled;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            @org.springframework.beans.factory.annotation.Value("${app.auth.password-reset.preview-enabled:${PASSWORD_RESET_PREVIEW_ENABLED:true}}") boolean passwordResetPreviewEnabled
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetPreviewEnabled = passwordResetPreviewEnabled;
    }

    private UserResponse convertDTO(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }


    @Override
    public Mono<PageResponse<UserResponse>> getAllUsers(PageRequestDto pageRequestDto) {
        return resolveAuthContext()
            .flatMap(authContext -> {
                requireAnyScope(authContext, "user.read", "user.write");
                requireAdmin(authContext);

                return Mono.fromCallable(() -> {
                    validateSortField(pageRequestDto.getSortBy());
                    Pageable pageable = pageRequestDto.getPageable();
                    Page<User> userPage = userRepository.findAll(pageable);

                    return PageResponse.<UserResponse>builder()
                        .content(userPage.getContent().stream()
                            .map(this::convertDTO)
                            .toList())
                        .pageNo(userPage.getNumber())
                        .pageSize(userPage.getSize())
                        .totalElements(userPage.getTotalElements())
                        .totalPage(userPage.getTotalPages())
                        .last(userPage.isLast())
                        .build();
                    })
                    .subscribeOn(Schedulers.boundedElastic());
            });
    }

    @Override
    public Mono<UserResponse> registerUser(CreateUserRequest user) {
        return Mono.fromCallable(() -> {
                    ensureUniqueEmail(user.getEmail());
                    ensureUniqueUsername(user.getUsername());

                    User u = new User();
                    u.setEmail(user.getEmail());
                    u.setUsername(user.getUsername());
                    u.setPassword(passwordEncoder.encode(user.getPassword()));
                    // Public registration must not allow privilege escalation.
                    u.setRole(0);
                    return convertDTO(userRepository.save(u));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<JwtResponse> login(LoginRequest request) {
        return Mono.fromCallable(() -> {
                    User user = userRepository.findByEmail(request.getEmail())
                            .orElseThrow(() -> new AppExceptions.UnauthorizedException("Invalid email or password"));

                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        throw new AppExceptions.UnauthorizedException("Invalid email or password");
                    }

                    return JwtResponse.builder()
                            .accessToken(jwtService.generateAccessToken(user))
                            .refreshToken(jwtService.generateRefreshToken(user))
                            .user(convertDTO(user))
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<JwtResponse> refreshAccessToken(String refreshToken) {
        return Mono.fromCallable(() -> {
                    try {
                        if (!StringUtils.hasText(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
                            throw new AppExceptions.UnauthorizedException("Invalid refresh token");
                        }

                        String userId = jwtService.extractUsername(refreshToken);
                        if (!jwtService.isValidToken(refreshToken, userId)) {
                            throw new AppExceptions.UnauthorizedException("Invalid refresh token");
                        }

                        User user = userRepository.findById(parseUserId(userId))
                                .orElseThrow(() -> new AppExceptions.UnauthorizedException("Invalid refresh token"));

                        return JwtResponse.builder()
                                .accessToken(jwtService.generateAccessToken(user))
                                .refreshToken(jwtService.generateRefreshToken(user))
                                .user(convertDTO(user))
                                .build();
                    } catch (AppExceptions.UnauthorizedException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        throw new AppExceptions.UnauthorizedException("Invalid refresh token", ex);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<PasswordResetResponse> requestPasswordReset(ForgotPasswordRequest request) {
        return Mono.fromCallable(() -> {
                    String previewToken = null;
                    User user = userRepository.findByEmail(request.getEmail()).orElse(null);

                    if (user != null) {
                        passwordResetTokenRepository.deleteByUserId(user.getId());

                        String rawToken = generateResetToken();
                        PasswordResetToken token = PasswordResetToken.builder()
                                .userId(user.getId())
                                .tokenHash(hashResetToken(rawToken))
                                .expiresAt(LocalDateTime.now().plusMinutes(PASSWORD_RESET_EXPIRY_MINUTES))
                                .build();
                        passwordResetTokenRepository.save(token);
                        previewToken = passwordResetPreviewEnabled ? rawToken : null;
                    }

                    return PasswordResetResponse.builder()
                            .message("If the email exists, a reset code has been issued")
                            .previewToken(previewToken)
                            .expiresInSeconds(PASSWORD_RESET_EXPIRY_MINUTES * 60L)
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> resetPassword(ResetPasswordRequest request) {
        return Mono.fromRunnable(() -> {
                    PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hashResetToken(request.getToken()))
                            .orElseThrow(() -> new AppExceptions.BadRequestException("Reset token is invalid or expired"));

                    if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
                        passwordResetTokenRepository.deleteByUserId(token.getUserId());
                        throw new AppExceptions.BadRequestException("Reset token is invalid or expired");
                    }

                    User user = userRepository.findById(token.getUserId())
                            .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("User not found"));
                    user.setPassword(passwordEncoder.encode(request.getPassword()));
                    userRepository.save(user);
                    passwordResetTokenRepository.deleteByUserId(user.getId());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Mono<Void> delete(String id) {
        UUID targetUserId = parseUserId(id);

        return resolveAuthContext()
                .flatMap(authContext -> {
                    requireAnyScope(authContext, "user.self", "user.write");
                    requireAdminOrOwner(authContext, targetUserId);

                    return Mono.fromRunnable(() -> {
                                if (!userRepository.existsById(targetUserId)) {
                                    throw new AppExceptions.ResourceNotFoundException("User not found");
                                }
                                userRepository.deleteById(targetUserId);
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .then();
                });
    }

    @Override
    public Mono<UserResponse> update(String id, UpdateUserRequest request) {
        UUID targetUserId = parseUserId(id);

        return resolveAuthContext()
                .flatMap(authContext -> {
                    requireAnyScope(authContext, "user.self", "user.write");
                    requireAdminOrOwner(authContext, targetUserId);

                    return Mono.fromCallable(() -> {
                                User user = userRepository.findById(targetUserId)
                                        .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("User not found"));

                                if (request.getUsername() != null) {
                                    validateTextField(request.getUsername(), "username");
                                    if (!request.getUsername().equals(user.getUsername())) {
                                        ensureUniqueUsername(request.getUsername());
                                        user.setUsername(request.getUsername());
                                    }
                                }

                                if (request.getEmail() != null) {
                                    validateTextField(request.getEmail(), "email");
                                    if (!request.getEmail().equals(user.getEmail())) {
                                        ensureUniqueEmail(request.getEmail());
                                        user.setEmail(request.getEmail());
                                    }
                                }

                                if (request.getPassword() != null) {
                                    validateTextField(request.getPassword(), "password");
                                    user.setPassword(passwordEncoder.encode(request.getPassword()));
                                }

                                if (request.getRole() != null) {
                                    requireAdmin(authContext);
                                    user.setRole(request.getRole());
                                }

                                return convertDTO(userRepository.save(user));
                            })
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }

    private void validateSortField(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new AppExceptions.BadRequestException("sortBy must be one of: " + ALLOWED_SORT_FIELDS);
        }
    }

    private void ensureUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new AppExceptions.ConflictException("Email already exists");
        }
    }

    private void ensureUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new AppExceptions.ConflictException("Username already exists");
        }
    }

    private void validateTextField(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new AppExceptions.BadRequestException(fieldName + " must not be blank");
        }
    }

    private String generateResetToken() {
        StringBuilder builder = new StringBuilder(8);
        for (int index = 0; index < 8; index++) {
            int randomIndex = secureRandom.nextInt(RESET_TOKEN_ALPHABET.length());
            builder.append(RESET_TOKEN_ALPHABET.charAt(randomIndex));
        }
        return builder.toString();
    }

    private String hashResetToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new AppExceptions.InternalServerErrorException("Failed to hash reset token", ex);
        }
    }

    private UUID parseUserId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new AppExceptions.BadRequestException("Invalid user id");
        }
    }

    private Mono<AuthContext> resolveAuthContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast)
                .map(this::toAuthContext)
                .switchIfEmpty(Mono.error(new AppExceptions.UnauthorizedException("Unauthorized")));
    }

    private AuthContext toAuthContext(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenantId");
        if (!StringUtils.hasText(tenantId)) {
            throw new AppExceptions.ForbiddenException("Token tenantId claim is missing");
        }

        Object userIdClaim = jwt.getClaims().get("userId");
        if (userIdClaim == null) {
            throw new AppExceptions.ForbiddenException("Token userId claim is missing");
        }

        UUID userId;
        try {
            userId = UUID.fromString(String.valueOf(userIdClaim));
        } catch (IllegalArgumentException ex) {
            throw new AppExceptions.ForbiddenException("Token userId claim is invalid");
        }

        Object roleClaim = jwt.getClaims().get("role");
        int role;
        if (roleClaim instanceof Number number) {
            role = number.intValue();
        } else {
            try {
                role = Integer.parseInt(String.valueOf(roleClaim));
            } catch (Exception ex) {
                throw new AppExceptions.ForbiddenException("Token role claim is invalid");
            }
        }

        Set<String> scopes = parseScopes(jwt.getClaims().get("scope"));
        return new AuthContext(userId, role, scopes, tenantId);
    }

    private Set<String> parseScopes(Object scopeClaim) {
        if (scopeClaim == null) {
            return Collections.emptySet();
        }

        if (scopeClaim instanceof String scopeText) {
            if (!StringUtils.hasText(scopeText)) {
                return Collections.emptySet();
            }
            return Arrays.stream(scopeText.trim().split("\\s+"))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        if (scopeClaim instanceof Iterable<?> iterable) {
            Set<String> scopes = new LinkedHashSet<>();
            for (Object value : iterable) {
                if (value != null && StringUtils.hasText(String.valueOf(value))) {
                    scopes.add(String.valueOf(value));
                }
            }
            return scopes;
        }

        return Collections.emptySet();
    }

    private void requireAnyScope(AuthContext authContext, String... expectedScopes) {
        for (String expectedScope : expectedScopes) {
            if (authContext.scopes().contains(expectedScope)) {
                return;
            }
        }
        throw new AppExceptions.ForbiddenException("Insufficient scope");
    }

    private void requireAdmin(AuthContext authContext) {
        if (!authContext.isAdmin()) {
            throw new AppExceptions.ForbiddenException("Admin role is required");
        }
    }

    private void requireAdminOrOwner(AuthContext authContext, UUID userId) {
        if (authContext.isAdmin()) {
            return;
        }
        if (!authContext.userId().equals(userId)) {
            throw new AppExceptions.ForbiddenException("You are not allowed to access this user");
        }
    }

    private record AuthContext(UUID userId, int role, Set<String> scopes, String tenantId) {
        boolean isAdmin() {
            return role >= ROLE_ADMIN;
        }
    }
}
