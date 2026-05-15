package codex.mmxxvi.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

public class CookieBearerTokenAuthenticationConverter implements ServerAuthenticationConverter {
    public static final String ACCESS_TOKEN_COOKIE = "vetau.auth.access";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String token = resolveFromAuthorizationHeader(exchange);

        if (token == null) {
            token = resolveFromCookie(exchange);
        }

        if (token == null || token.isBlank()) {
            return Mono.empty();
        }

        return Mono.just(new BearerTokenAuthenticationToken(token));
    }

    private String resolveFromAuthorizationHeader(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private String resolveFromCookie(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(ACCESS_TOKEN_COOKIE);

        if (cookie == null) {
            return null;
        }

        return cookie.getValue();
    }
}
