package codex.mmxxvi.controller;


import java.util.UUID;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import codex.mmxxvi.dto.internal.AdjustTicketStockRequest;
import codex.mmxxvi.dto.internal.TicketItemSnapshotResponse;
import codex.mmxxvi.dto.request.CreateTicketRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.UpdateTicketItemsRequest;
import codex.mmxxvi.dto.request.UpdateTicketRequest;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.ResponseTicket;
import codex.mmxxvi.service.TicketService;
import codex.mmxxvi.support.InternalApiKeyService;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RequestMapping("/v1")
@RestController
public class TicketController {
    private final TicketService ticketService;
    private final InternalApiKeyService internalApiKeyService;

    public TicketController(TicketService ticketService, InternalApiKeyService internalApiKeyService) {
        this.ticketService = ticketService;
        this.internalApiKeyService = internalApiKeyService;
    }


    @GetMapping("/tickets")
    public Mono<PageResponse<ResponseTicket>> getTickets(PageRequestDto pageRequestDto){
        return ticketService.getTickets(pageRequestDto);
    }
    @GetMapping("/tickets/{id}")
    public Mono<ResponseTicket> getTicket(@PathVariable UUID id) {
        return ticketService.getTicket(id);
    }
    @PostMapping("/tickets")
    public Mono<ResponseTicket> createTicket(@Valid @RequestBody CreateTicketRequest createTicketRequest) {
        return ticketService.createTicket(createTicketRequest);
    }
    @PatchMapping("/tickets/{id}")
    public Mono<ResponseTicket> updateTicket(@PathVariable UUID id, @Valid @RequestBody UpdateTicketRequest updateTicketRequest) {
        return ticketService.updateTicket(id, updateTicketRequest);
    }
    @PutMapping("/tickets/{id}/items")
    public Mono<ResponseTicket> updateTicketItems(@PathVariable UUID id, @Valid @RequestBody UpdateTicketItemsRequest updateTicketItemsRequest) {
        return ticketService.updateTicketItems(id, updateTicketItemsRequest);
    }
    @DeleteMapping("/tickets/{id}")
    public Mono<Void> deleteTicket(@PathVariable UUID id) {
        return ticketService.deleteTicket(id);
    }

    @GetMapping("/internal/ticket-items/{ticketItemId}")
    public Mono<TicketItemSnapshotResponse> getTicketItemSnapshot(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable UUID ticketItemId
    ) {
        internalApiKeyService.validate(internalKey);
        return ticketService.getTicketItemSnapshot(ticketItemId);
    }

    @PostMapping("/internal/ticket-items/{ticketItemId}/reserve")
    public Mono<TicketItemSnapshotResponse> reserveTicketItem(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable UUID ticketItemId,
            @Valid @RequestBody AdjustTicketStockRequest request
    ) {
        internalApiKeyService.validate(internalKey);
        return ticketService.reserveTicketItem(ticketItemId, request);
    }

    @PostMapping("/internal/ticket-items/{ticketItemId}/release")
    public Mono<TicketItemSnapshotResponse> releaseTicketItem(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable UUID ticketItemId,
            @Valid @RequestBody AdjustTicketStockRequest request
    ) {
        internalApiKeyService.validate(internalKey);
        return ticketService.releaseTicketItem(ticketItemId, request);
    }
}
