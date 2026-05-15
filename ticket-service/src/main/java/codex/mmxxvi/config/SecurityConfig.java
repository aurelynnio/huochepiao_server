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
                    .pathMatchers(HttpMethod.GET, "/v1/tickets", "/v1/tickets/**")
                    .hasAnyAuthority("SCOPE_ticket.read", "SCOPE_ticket.write")
                    .pathMatchers(HttpMethod.PATCH, "/v1/tickets/**")
                    .hasAuthority("SCOPE_ticket.write")
                    .pathMatchers(HttpMethod.PUT, "/v1/tickets/**")
                    .hasAuthority("SCOPE_ticket.write")
                    .pathMatchers(HttpMethod.DELETE, "/v1/tickets/**")
                    .hasAuthority("SCOPE_ticket.write")
                    .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }))
                .build();
    }
}
