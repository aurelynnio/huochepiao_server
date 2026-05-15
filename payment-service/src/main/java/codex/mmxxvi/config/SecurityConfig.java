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
                        .pathMatchers(HttpMethod.GET, "/v1/payments/vnpay/callback", "/v1/payments/vnpay-payment", "/v1/vnpay-payment")
                        .permitAll()
                        .pathMatchers(HttpMethod.GET, "/v1/payments", "/v1/payments/**", "/v1/*")
                        .hasAnyAuthority("SCOPE_payment.read", "SCOPE_payment.read.self")
                        .pathMatchers(HttpMethod.POST, "/v1/payments/*/refund", "/v1/refund")
                        .hasAnyAuthority("SCOPE_payment.refund", "SCOPE_payment.refund.self")
                        .pathMatchers(HttpMethod.POST, "/v1/payments", "/v1/create")
                        .hasAnyAuthority("SCOPE_payment.write", "SCOPE_payment.write.self")
                        .anyExchange()
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }))
                .build();
    }
}
