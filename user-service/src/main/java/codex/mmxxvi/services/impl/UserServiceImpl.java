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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "username", "email", "role");
    private static final int ROLE_ADMIN = 1;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
