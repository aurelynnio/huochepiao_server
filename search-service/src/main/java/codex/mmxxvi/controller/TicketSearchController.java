package codex.mmxxvi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.SearchTicketRequest;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.SearchTicketResponse;
import codex.mmxxvi.services.TicketSearchService;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/search/tickets")
public class TicketSearchController {

    private final TicketSearchService ticketSearchService;

    public TicketSearchController(TicketSearchService ticketSearchService) {
        this.ticketSearchService = ticketSearchService;
    }

    @GetMapping({"/", ""})
    public Mono<PageResponse<SearchTicketResponse>> searchTickets(
            @RequestParam(name = "q", required = false) String keyword,
            @Valid @ModelAttribute SearchTicketRequest searchTicketRequest,
            @Valid @ModelAttribute PageRequestDto pageRequestDto
    ) {
        return ticketSearchService.searchTickets(keyword, searchTicketRequest, pageRequestDto);
    }
}
