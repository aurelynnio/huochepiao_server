package codex.mmxxvi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {
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
