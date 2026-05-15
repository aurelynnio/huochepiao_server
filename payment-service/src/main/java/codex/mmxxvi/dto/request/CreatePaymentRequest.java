package codex.mmxxvi.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    @NotNull(message = "Order id is required")
    private UUID orderId;

    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount must be greater than or equal to 0")
    private Long amount;

    @NotBlank(message = "Payment method is required")
    @Size(max = 36, message = "Payment method must be at most 36 characters")
    private String paymentMethod;

    @Min(value = 0, message = "Status must be greater than or equal to 0")
    private Integer status;

    private UUID transactionId;

    private LocalDateTime paidAt;
}
