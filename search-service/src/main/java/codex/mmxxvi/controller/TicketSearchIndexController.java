package codex.mmxxvi.controller;

import codex.mmxxvi.dto.request.IndexTicketRequest;
import codex.mmxxvi.services.TicketSearchService;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import codex.mmxxvi.support.InternalApiKeyService;

@RestController
@RequestMapping("/internal/search/tickets")
public class TicketSearchIndexController {

    private final TicketSearchService ticketSearchService;
    private final InternalApiKeyService internalApiKeyService;

    public TicketSearchIndexController(
            TicketSearchService ticketSearchService,
            InternalApiKeyService internalApiKeyService
    ) {
        this.ticketSearchService = ticketSearchService;
        this.internalApiKeyService = internalApiKeyService;
    }

    @PostMapping({"/", ""})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> indexTicket(
            @RequestHeader("X-Internal-Key") String internalKey,
            @Valid @RequestBody IndexTicketRequest request
    ) {
        internalApiKeyService.validate(internalKey);
        return ticketSearchService.indexTicket(request);
    }

    @DeleteMapping("/{ticketId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTicket(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable UUID ticketId
    ) {
        internalApiKeyService.validate(internalKey);
        return ticketSearchService.deleteTicket(ticketId);
    }
}
