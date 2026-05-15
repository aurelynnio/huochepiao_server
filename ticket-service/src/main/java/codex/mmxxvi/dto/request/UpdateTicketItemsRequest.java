package codex.mmxxvi.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketItemsRequest {
    @Valid
    private List<UpdateTicketItemRequest> ticketItems;

    public List<UpdateTicketItemRequest> getTicketItems() {
        return ticketItems;
    }
}
