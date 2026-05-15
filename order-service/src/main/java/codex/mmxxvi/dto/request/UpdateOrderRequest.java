package codex.mmxxvi.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateOrderRequest {
    private UUID id;
    private UUID userId;
    private UUID ticketItemId;
    private Integer quantity;
    private Integer unitPrice;
    private Long totalPrice;
    @NotNull(message = "Status is required")
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
