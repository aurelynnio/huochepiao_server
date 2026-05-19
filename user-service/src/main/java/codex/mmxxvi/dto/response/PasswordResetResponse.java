package codex.mmxxvi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PasswordResetResponse {
    private String message;
    private String previewToken;
    private Long expiresInSeconds;
}
