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
                    .pathMatchers(HttpMethod.GET, "/v1/orders", "/v1/orders/**")
                    .hasAnyAuthority("SCOPE_order.read", "SCOPE_order.read.self", "SCOPE_order.admin")
                    .pathMatchers(HttpMethod.POST, "/v1/orders", "/v1/orders/**")
                    .hasAnyAuthority("SCOPE_order.write", "SCOPE_order.write.self", "SCOPE_order.admin")
                    .pathMatchers(HttpMethod.PATCH, "/v1/orders/**")
                    .hasAuthority("SCOPE_order.admin")
                    .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }))
                .build();
    }
}
