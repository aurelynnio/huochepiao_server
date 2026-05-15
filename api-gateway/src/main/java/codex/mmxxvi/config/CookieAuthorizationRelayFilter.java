package codex.mmxxvi.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class CookieAuthorizationRelayFilter implements GlobalFilter, Ordered {
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION) != null) {
            return chain.filter(exchange);
        }

        HttpCookie accessToken = exchange.getRequest()
                .getCookies()
                .getFirst(CookieBearerTokenAuthenticationConverter.ACCESS_TOKEN_COOKIE);

        if (accessToken == null || accessToken.getValue().isBlank()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken.getValue())
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
