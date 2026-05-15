package codex.mmxxvi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String issuer;
    private String keyId;
    private String privateKey;
    private String publicKey;
    private String defaultTenantId;
    private long accessExpiration;
    private long refreshExpiration;
}
