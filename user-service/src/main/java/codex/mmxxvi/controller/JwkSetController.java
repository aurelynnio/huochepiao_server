package codex.mmxxvi.controller;

import codex.mmxxvi.config.JwtKeyManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwkSetController {

    private final JwtKeyManager jwtKeyManager;

    public JwkSetController(JwtKeyManager jwtKeyManager) {
        this.jwtKeyManager = jwtKeyManager;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwkSet() {
        return jwtKeyManager.getJwkSetAsMap();
    }
}
