package codex.mmxxvi.service;

import java.util.UUID;

import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.UpdateTicketItemsRequest;
import codex.mmxxvi.dto.request.UpdateTicketRequest;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.ResponseTicket;
import reactor.core.publisher.Mono;

public interface TicketService {
    Mono<PageResponse<ResponseTicket>> getTickets(PageRequestDto pageRequestDto);
    Mono<ResponseTicket> getTicket(UUID ticketId);
    Mono<ResponseTicket> createTicket();
    Mono<ResponseTicket> updateTicket(UUID ticketId, UpdateTicketRequest updateTicketRequest);
    Mono<ResponseTicket> updateTicketItems(UUID ticketId, UpdateTicketItemsRequest updateTicketItemsRequest);
    Mono<Void> deleteTicket(UUID ticketId);
}
