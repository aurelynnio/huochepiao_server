package codex.mmxxvi.dto.request;

import java.util.UUID;
import java.util.List;

import jakarta.validation.Valid;
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
public class CreateOrderRequest {
    @NotNull(message = "User id is required")
    private UUID userId;

    @NotNull(message = "Ticket item id is required")
    private UUID ticketItemId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    private Long unitPrice;

    private Long totalPrice;

    @Min(value = 0, message = "Status must be greater than or equal to 0")
    private Integer status;

    @Valid
    @Builder.Default
    private List<CreateOrderPassengerRequest> passengers = List.of();
}
