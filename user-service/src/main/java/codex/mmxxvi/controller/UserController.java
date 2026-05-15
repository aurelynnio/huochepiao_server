package codex.mmxxvi.controller;

import codex.mmxxvi.dto.request.CreateUserRequest;
import codex.mmxxvi.dto.request.LoginRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.UpdateUserRequest;
import codex.mmxxvi.config.JwtProperties;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.UserResponse;
import codex.mmxxvi.services.UserService;
import jakarta.validation.Valid;

import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1")
public class UserController {
    private static final String ACCESS_TOKEN_COOKIE = "vetau.auth.access";
    private static final String REFRESH_TOKEN_COOKIE = "vetau.auth.refresh";

    private final UserService userService;
    private final JwtProperties jwtProperties;

    public UserController(UserService userService, JwtProperties jwtProperties) {
        this.userService = userService;
        this.jwtProperties = jwtProperties;
    }

    @GetMapping("/users")
    public Mono<PageResponse<UserResponse>> getUsers(@Valid @ModelAttribute PageRequestDto pageRequestDto) {
        return userService.getAllUsers(pageRequestDto);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponse> registerUser(@Valid @RequestBody CreateUserRequest user) {
        return userService.registerUser(user);
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<UserResponse>> login(@Valid @RequestBody LoginRequest req, ServerWebExchange exchange) {
        return userService.login(req)
                .map(session -> {
                    addCookie(
                            exchange,
                            ACCESS_TOKEN_COOKIE,
                            session.getAccessToken(),
                            Duration.ofMillis(jwtProperties.getAccessExpiration())
                    );
                    addCookie(
                            exchange,
                            REFRESH_TOKEN_COOKIE,
                            session.getRefreshToken(),
                            Duration.ofMillis(jwtProperties.getRefreshExpiration())
                    );

                    return ResponseEntity.ok(session.getUser());
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(ServerWebExchange exchange) {
        expireCookie(exchange, ACCESS_TOKEN_COOKIE);
        expireCookie(exchange, REFRESH_TOKEN_COOKIE);

        return Mono.just(ResponseEntity.noContent().build());
    }

    @DeleteMapping("/users/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return userService.delete(id)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PutMapping("/users/{id}")
    public Mono<UserResponse> update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    private void addCookie(ServerWebExchange exchange, String name, String value, Duration maxAge) {
        exchange.getResponse().addCookie(ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(isSecure(exchange))
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build());
    }

    private void expireCookie(ServerWebExchange exchange, String name) {
        exchange.getResponse().addCookie(ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(isSecure(exchange))
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build());
    }

    private boolean isSecure(ServerWebExchange exchange) {
        String forwardedProto = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");

        if (forwardedProto != null) {
            return "https".equalsIgnoreCase(forwardedProto);
        }

        return "https".equalsIgnoreCase(exchange.getRequest().getURI().getScheme());
    }

}
