package codex.mmxxvi.dto.internal;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InternalOrderResponse {
    private UUID id;
    private UUID userId;
    private UUID ticketItemId;
    private Integer quantity;
    private Long unitPrice;
    private Long totalPrice;
    private String ticketCode;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
