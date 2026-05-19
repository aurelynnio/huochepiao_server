package codex.mmxxvi.integration.search;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import codex.mmxxvi.dto.integration.search.IndexTicketRequest;

@FeignClient(
        name = "search-service",
        url = "${SEARCH_SERVICE_URL:http://search-service:8085}",
        path = "/internal/search/tickets"
)
public interface TicketSearchIndexClient {

    @PostMapping
    void indexTicket(
            @RequestHeader("X-Internal-Key") String internalApiKey,
            @RequestBody IndexTicketRequest request
    );

    @DeleteMapping("/{ticketId}")
    void deleteTicket(
            @RequestHeader("X-Internal-Key") String internalApiKey,
            @PathVariable UUID ticketId
    );
}
