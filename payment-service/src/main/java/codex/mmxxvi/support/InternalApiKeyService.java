package codex.mmxxvi.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InternalApiKeyService {
    private final String internalApiKey;

    public InternalApiKeyService(
            @Value("${app.internal.api-key:${APP_INTERNAL_API_KEY:vetau-internal-key}}") String internalApiKey
    ) {
        this.internalApiKey = internalApiKey;
    }

    public String getInternalApiKey() {
        return internalApiKey;
    }
}
