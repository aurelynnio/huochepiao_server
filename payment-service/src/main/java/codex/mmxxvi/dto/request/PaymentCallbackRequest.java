package codex.mmxxvi.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {
    @NotBlank(message = "Transaction id is required")
    @Size(max = 36, message = "Transaction id must be at most 36 characters")
    private String transactionId;

    @NotNull(message = "Status is required")
    @Min(value = 0, message = "Status must be greater than or equal to 0")
    private Integer status;

    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount must be greater than or equal to 0")
    private Long amount;

    @Size(max = 36, message = "Payment method must be at most 36 characters")
    private String paymentMethod;

    @Size(max = 50, message = "Gateway response code must be at most 50 characters")
    private String gatewayResponseCode;

    private LocalDateTime paidAt;
}
