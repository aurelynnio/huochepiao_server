package codex.mmxxvi.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtKeyManagerTest {

    @Test
    void createsEphemeralKeyPairAndPublicJwkWhenPemKeysAreMissing() {
        JwtProperties properties = new JwtProperties();
        properties.setKeyId("test-key");

        JwtKeyManager keyManager = new JwtKeyManager(properties);
        Map<String, Object> jwkSet = keyManager.getJwkSetAsMap();

        assertThat(keyManager.getPrivateKey()).isNotNull();
        assertThat(keyManager.getPublicKey()).isNotNull();
        assertThat(jwkSet).containsKey("keys");

        Object keys = jwkSet.get("keys");
        assertThat(keys).isInstanceOf(List.class);
        assertThat((List<?>) keys).hasSize(1);
        Map<?, ?> jwk = (Map<?, ?>) ((List<?>) keys).getFirst();
        assertThat(jwk.get("kid")).isEqualTo("test-key");
        assertThat(jwk.get("kty")).isEqualTo("RSA");
    }
}
