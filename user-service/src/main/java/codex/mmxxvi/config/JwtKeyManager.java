package codex.mmxxvi.config;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

@Component
public class JwtKeyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtKeyManager.class);

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final RSAKey publicRsaJwk;

    public JwtKeyManager(JwtProperties jwtProperties) {
        KeyPair keyPair = resolveKeyPair(jwtProperties);
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();

        String keyId = StringUtils.hasText(jwtProperties.getKeyId())
                ? jwtProperties.getKeyId().trim()
                : "user-service-key-1";
        this.publicRsaJwk = new RSAKey.Builder(publicKey)
                .keyID(keyId)
                .build();
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public Map<String, Object> getJwkSetAsMap() {
        return new JWKSet(publicRsaJwk).toJSONObject();
    }

    private KeyPair resolveKeyPair(JwtProperties jwtProperties) {
        String privateKeyPem = jwtProperties.getPrivateKey();
        String publicKeyPem = jwtProperties.getPublicKey();

        if (StringUtils.hasText(privateKeyPem) && StringUtils.hasText(publicKeyPem)) {
            try {
                RSAPrivateKey parsedPrivateKey = parsePrivateKey(privateKeyPem);
                RSAPublicKey parsedPublicKey = parsePublicKey(publicKeyPem);
                return new KeyPair(parsedPublicKey, parsedPrivateKey);
            } catch (GeneralSecurityException ex) {
                throw new IllegalStateException("Invalid JWT key pair configuration", ex);
            }
        }

        LOGGER.warn("JWT private/public key is not configured; generating ephemeral RSA key pair for current runtime");
        return generateEphemeralKeyPair();
    }

    private KeyPair generateEphemeralKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to generate JWT RSA key pair", ex);
        }
    }

    private RSAPrivateKey parsePrivateKey(String pem) throws GeneralSecurityException {
        String normalized = normalizePem(pem)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private RSAPublicKey parsePublicKey(String pem) throws GeneralSecurityException {
        String normalized = normalizePem(pem)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private String normalizePem(String pem) {
        return pem
                .replace("\\r", "")
                .replace("\\n", "")
                .replaceAll("\\s+", "");
    }
}
