package codex.mmxxvi.services.impl;

import codex.mmxxvi.config.JwtKeyManager;
import codex.mmxxvi.config.JwtProperties;
import codex.mmxxvi.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private JwtKeyManager keyManager;
    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("http://user-service:8081");
        properties.setKeyId("test-key");
        properties.setDefaultTenantId("tenant-a");
        properties.setAccessExpiration(60_000);
        properties.setRefreshExpiration(120_000);

        keyManager = new JwtKeyManager(properties);
        jwtService = new JwtServiceImpl(properties, keyManager);
    }

    @Test
    void generateAccessTokenIncludesRequiredClaimsAndUserScopes() {
        String token = jwtService.generateAccessToken(user(0));

        Claims claims = parse(token);

        assertThat(claims.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(claims.getIssuer()).isEqualTo("http://user-service:8081");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.get("tenantId", String.class)).isEqualTo("tenant-a");
        assertThat(claims.get("scope", String.class))
                .contains("user.self")
                .doesNotContain("user.write");
        assertThat(jwtService.isValidToken(token, USER_ID.toString())).isTrue();
    }

    @Test
    void generateRefreshTokenUsesRefreshType() {
        String token = jwtService.generateRefreshToken(user(0));

        assertThat(jwtService.extractUsername(token)).isEqualTo(USER_ID.toString());
        assertThat(jwtService.extractTokenType(token)).isEqualTo("refresh");
        assertThat(jwtService.isRefreshToken(token)).isTrue();
    }

    @Test
    void adminTokenContainsAdministrativeScopes() {
        Claims claims = parse(jwtService.generateAccessToken(user(1)));

        assertThat(claims.get("scope", String.class))
                .contains("user.write", "order.admin", "payment.refund", "ticket.write", "search.index");
    }

    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(keyManager.getPublicKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private User user(int role) {
        return User.builder()
                .id(USER_ID)
                .username("alice")
                .email("alice@example.com")
                .password("encoded")
                .role(role)
                .build();
    }
}
