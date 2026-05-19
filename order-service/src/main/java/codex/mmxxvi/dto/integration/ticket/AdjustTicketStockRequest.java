package codex.mmxxvi.dto.integration.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustTicketStockRequest {
    private Integer quantity;
    private List<String> seatLabels;
}
