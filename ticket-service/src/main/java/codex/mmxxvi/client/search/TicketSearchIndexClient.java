package codex.mmxxvi.client.search;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import codex.mmxxvi.client.search.dto.IndexTicketRequest;

@FeignClient(name = "search-service", path = "/internal/search/tickets")
public interface TicketSearchIndexClient {

    @PostMapping
    void indexTicket(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody IndexTicketRequest request
    );

    @DeleteMapping("/{ticketId}")
    void deleteTicket(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable UUID ticketId
    );
}
