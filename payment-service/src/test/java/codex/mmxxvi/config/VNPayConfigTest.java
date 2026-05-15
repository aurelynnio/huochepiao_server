package codex.mmxxvi.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VNPayConfigTest {

    @Test
    void digestHelpersReturnExpectedHashes() {
        assertThat(VNPayConfig.md5("hello")).isEqualTo("5d41402abc4b2a76b9719d911017c592");
        assertThat(VNPayConfig.Sha256("hello"))
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void hashAllFieldsSortsFieldsBeforeSigning() {
        VNPayConfig config = new VNPayConfig();
        config.setHashSecret("secret");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("vnp_TxnRef", "txn-1");
        fields.put("vnp_Amount", "10000");

        assertThat(config.hashAllFields(fields))
                .isEqualTo(VNPayConfig.hmacSHA512("secret", "vnp_Amount=10000&vnp_TxnRef=txn-1"));
    }

    @Test
    void getIpAddressPrefersFirstForwardedAddress() {
        String ip = VNPayConfig.getIpAddress(MockServerHttpRequest
                .get("http://localhost")
                .header("x-forwarded-for", "203.0.113.10, 203.0.113.11")
                .build());

        assertThat(ip).isEqualTo("203.0.113.10");
    }
}
