package codex.mmxxvi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/users/register", "/api/users/login", "/api/users/logout").permitAll()
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/payments/vnpay/callback",
                                "/api/payments/vnpay-payment",
                                "/api/payment/vnpay/callback",
                                "/api/payment/vnpay-payment",
                                "/api/search/tickets",
                                "/api/search/tickets/**"
                        ).permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/users", "/api/users/**")
                        .hasAnyAuthority("SCOPE_user.read", "SCOPE_user.write", "SCOPE_user.self")
                        .pathMatchers(HttpMethod.PUT, "/api/users/**")
                        .hasAnyAuthority("SCOPE_user.write", "SCOPE_user.self")
                        .pathMatchers(HttpMethod.DELETE, "/api/users/**")
                        .hasAnyAuthority("SCOPE_user.write", "SCOPE_user.self")
                        .pathMatchers(HttpMethod.GET, "/api/orders", "/api/orders/**", "/api/order", "/api/order/**")
                        .hasAnyAuthority("SCOPE_order.read", "SCOPE_order.read.self", "SCOPE_order.admin")
                        .pathMatchers(HttpMethod.POST, "/api/orders", "/api/orders/**", "/api/order", "/api/order/**")
                        .hasAnyAuthority("SCOPE_order.write", "SCOPE_order.write.self", "SCOPE_order.admin")
                        .pathMatchers(HttpMethod.PATCH, "/api/orders/**", "/api/order/**")
                        .hasAuthority("SCOPE_order.admin")
                        .pathMatchers(HttpMethod.GET, "/api/payments", "/api/payments/**", "/api/payment", "/api/payment/**")
                        .hasAnyAuthority("SCOPE_payment.read", "SCOPE_payment.read.self")
                        .pathMatchers(HttpMethod.POST, "/api/payments", "/api/payments/**", "/api/payment", "/api/payment/**")
                        .hasAnyAuthority("SCOPE_payment.write", "SCOPE_payment.write.self", "SCOPE_payment.refund", "SCOPE_payment.refund.self")
                        .pathMatchers(HttpMethod.GET, "/api/tickets", "/api/tickets/**", "/api/ticket", "/api/ticket/**")
                        .hasAnyAuthority("SCOPE_ticket.read", "SCOPE_ticket.write")
                        .pathMatchers(HttpMethod.POST, "/api/tickets", "/api/tickets/**", "/api/ticket", "/api/ticket/**")
                        .hasAuthority("SCOPE_ticket.write")
                        .pathMatchers(HttpMethod.PATCH, "/api/tickets/**", "/api/ticket/**")
                        .hasAuthority("SCOPE_ticket.write")
                        .pathMatchers(HttpMethod.PUT, "/api/tickets/**", "/api/ticket/**")
                        .hasAuthority("SCOPE_ticket.write")
                        .pathMatchers(HttpMethod.DELETE, "/api/tickets/**", "/api/ticket/**")
                        .hasAuthority("SCOPE_ticket.write")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenConverter(new CookieBearerTokenAuthenticationConverter())
                        .jwt(jwt -> {
                        }))
                .build();
    }
}
