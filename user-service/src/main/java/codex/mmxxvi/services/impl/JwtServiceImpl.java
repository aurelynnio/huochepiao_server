package codex.mmxxvi.services.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import codex.mmxxvi.config.JwtKeyManager;
import codex.mmxxvi.config.JwtProperties;
import codex.mmxxvi.entity.User;
import codex.mmxxvi.services.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {
    private static final String ADMIN_SCOPES = String.join(" ",
            "user.read",
            "user.write",
            "user.self",
            "order.read",
            "order.write",
            "order.read.self",
            "order.write.self",
            "order.admin",
            "payment.read",
            "payment.write",
            "payment.read.self",
            "payment.write.self",
            "payment.refund",
            "payment.refund.self",
            "ticket.read",
            "ticket.write",
            "search.index"
    );
    private static final String USER_SCOPES = String.join(" ",
            "user.self",
            "order.read.self",
            "order.write.self",
            "payment.read.self",
            "payment.write.self",
            "payment.refund.self",
            "ticket.read"
    );

    private final JwtProperties jwtProperties;
    private final JwtKeyManager jwtKeyManager;

    private String generateToken(User user, long expiration, String tokenType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", tokenType);
        claims.put("userId", user.getId().toString());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        claims.put("email", user.getEmail());
        claims.put("tenantId", resolveTenantId());
        claims.put("scope", resolveScopes(user.getRole()));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId().toString())
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(jwtKeyManager.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtKeyManager.getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public String generateAccessToken(User user) {
        return generateToken(user, jwtProperties.getAccessExpiration(), "access");
    }

    @Override
    public String generateRefreshToken(User user) {
        return generateToken(user, jwtProperties.getRefreshExpiration(), "refresh");
    }

    @Override
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Override
    public String extractTokenType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    @Override
    public boolean isValidToken(String token, String username) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject().equals(username)
                && claims.getExpiration().after(new Date());
    }

    @Override
    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }

    private String resolveTenantId() {
        String tenantId = jwtProperties.getDefaultTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return "public";
        }
        return tenantId.trim();
    }

    private String resolveScopes(Integer role) {
        int roleValue = Optional.ofNullable(role).orElse(0);
        if (roleValue >= 1) {
            return ADMIN_SCOPES;
        }
        return USER_SCOPES;
    }
}
