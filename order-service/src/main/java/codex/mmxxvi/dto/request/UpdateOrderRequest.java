package codex.mmxxvi.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Min(value = 0, message = "Status must be greater than or equal to 0")
    @Max(value = 4, message = "Status must be less than or equal to 4")
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
