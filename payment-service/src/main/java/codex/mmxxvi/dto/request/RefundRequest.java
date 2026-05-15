package codex.mmxxvi.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    @NotNull(message = "Payment id is required")
    private UUID paymentId;

    @NotNull(message = "Refund amount is required")
    @Min(value = 0, message = "Refund amount must be greater than or equal to 0")
    private Long refundAmount;
}
