package codex.mmxxvi.dto.integration.order;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class InternalOrderResponse {
    private UUID id;
    private UUID userId;
    private UUID ticketItemId;
    private Integer quantity;
    private Long unitPrice;
    private Long totalPrice;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
