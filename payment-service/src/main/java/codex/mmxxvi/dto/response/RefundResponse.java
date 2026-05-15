package codex.mmxxvi.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private UUID paymentId;
    private UUID orderId;
    private Long refundAmount;
    private String paymentMethod;
    private String transactionId;
    private Integer status;
    private LocalDateTime refundedAt;
}
