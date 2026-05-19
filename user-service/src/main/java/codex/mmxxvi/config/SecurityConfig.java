package codex.mmxxvi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.POST, "/v1/register", "/v1/login", "/v1/logout", "/v1/refresh", "/v1/forgot-password", "/v1/reset-password").permitAll()
                    .pathMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll()
                    .pathMatchers(HttpMethod.GET, "/v1/users", "/v1/users/**")
                    .hasAnyAuthority("SCOPE_user.read", "SCOPE_user.write")
                    .pathMatchers(HttpMethod.PUT, "/v1/users/**")
                    .hasAnyAuthority("SCOPE_user.self", "SCOPE_user.write")
                    .pathMatchers(HttpMethod.DELETE, "/v1/users/**")
                    .hasAnyAuthority("SCOPE_user.self", "SCOPE_user.write")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }))
                .build();
    }
}
