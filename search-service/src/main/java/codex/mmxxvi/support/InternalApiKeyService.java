package codex.mmxxvi.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import codex.mmxxvi.exception.AppExceptions;

@Component
public class InternalApiKeyService {
    private final String internalApiKey;

    public InternalApiKeyService(
            @Value("${app.internal.api-key:${APP_INTERNAL_API_KEY:vetau-internal-key}}") String internalApiKey
    ) {
        this.internalApiKey = internalApiKey;
    }

    public void validate(String providedKey) {
        if (!StringUtils.hasText(providedKey) || !internalApiKey.equals(providedKey)) {
            throw new AppExceptions.ForbiddenException("Invalid internal api key");
        }
    }
}
