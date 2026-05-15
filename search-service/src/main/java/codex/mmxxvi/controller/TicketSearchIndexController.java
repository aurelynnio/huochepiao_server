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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/search/tickets")
public class TicketSearchIndexController {

    private final TicketSearchService ticketSearchService;

    public TicketSearchIndexController(TicketSearchService ticketSearchService) {
        this.ticketSearchService = ticketSearchService;
    }

    @PostMapping({"/", ""})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> indexTicket(@Valid @RequestBody IndexTicketRequest request) {
        return ticketSearchService.indexTicket(request);
    }

    @DeleteMapping("/{ticketId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTicket(@PathVariable UUID ticketId) {
        return ticketSearchService.deleteTicket(ticketId);
    }
}
