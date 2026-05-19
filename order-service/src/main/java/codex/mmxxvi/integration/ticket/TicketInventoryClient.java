package codex.mmxxvi.integration.ticket;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import codex.mmxxvi.dto.integration.ticket.AdjustTicketStockRequest;
import codex.mmxxvi.dto.integration.ticket.TicketItemSnapshotResponse;

@FeignClient(
        name = "ticket-service",
        url = "${TICKET_SERVICE_URL:http://ticket-service:8086}",
        path = "/v1/internal/ticket-items"
)
public interface TicketInventoryClient {

    @GetMapping("/{ticketItemId}")
    TicketItemSnapshotResponse getTicketItem(
            @RequestHeader("X-Internal-Key") String internalApiKey,
            @PathVariable UUID ticketItemId
    );

    @PostMapping("/{ticketItemId}/reserve")
    TicketItemSnapshotResponse reserveTicketItem(
            @RequestHeader("X-Internal-Key") String internalApiKey,
            @PathVariable UUID ticketItemId,
            @RequestBody AdjustTicketStockRequest request
    );

    @PostMapping("/{ticketItemId}/release")
    TicketItemSnapshotResponse releaseTicketItem(
            @RequestHeader("X-Internal-Key") String internalApiKey,
            @PathVariable UUID ticketItemId,
            @RequestBody AdjustTicketStockRequest request
    );
}
