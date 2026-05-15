package codex.mmxxvi.services;

import java.util.UUID;

import codex.mmxxvi.dto.request.IndexTicketRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.SearchTicketResponse;
import reactor.core.publisher.Mono;

public interface TicketSearchService {
    Mono<PageResponse<SearchTicketResponse>> searchTickets(String keyword, PageRequestDto pageRequestDto);
    Mono<Void> indexTicket(IndexTicketRequest request);
    Mono<Void> deleteTicket(UUID ticketId);
}
