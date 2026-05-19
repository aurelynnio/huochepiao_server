package codex.mmxxvi.services.impl;

import codex.mmxxvi.dto.request.IndexTicketRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.SearchTicketRequest;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.SearchTicketResponse;
import codex.mmxxvi.entity.SearchTicketDocument;
import codex.mmxxvi.exception.AppExceptions;
import com.mongodb.client.result.DeleteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketSearchServiceImplTest {

    private static final UUID TICKET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private ReactiveMongoTemplate mongoTemplate;
    private TicketSearchServiceImpl service;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(ReactiveMongoTemplate.class);
        service = new TicketSearchServiceImpl(mongoTemplate, new ObjectMapper(), "tickets");
    }

    @Test
    void searchTicketsMapsMongoDocumentsToPageResponse() {
        SearchTicketRequest request = new SearchTicketRequest();
        request.setDepartureStation("Sai Gon");
        request.setSeatClass("Khoang 4");

        SearchTicketDocument document = SearchTicketDocument.builder()
                .id(TICKET_ID)
                .title("Rock Night")
                .status(0)
                .ticketItems(List.of())
                .build();

        when(mongoTemplate.count(any(Query.class), eq(SearchTicketDocument.class), eq("tickets")))
                .thenReturn(Mono.just(1L));
        when(mongoTemplate.find(any(Query.class), eq(SearchTicketDocument.class), eq("tickets")))
                .thenReturn(Flux.just(document));

        PageResponse<SearchTicketResponse> response = service
                .searchTickets("rock", request, new PageRequestDto(0, 10))
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getId()).isEqualTo(TICKET_ID);
        assertThat(response.getContent().getFirst().getScore()).isNull();
        assertThat(response.getTotalElements()).isEqualTo(1);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(SearchTicketDocument.class), eq("tickets"));
        String queryJson = queryCaptor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains("departureStationName");
        assertThat(queryJson).contains("seatClass");
        assertThat(queryJson).contains("stockAvailable");
    }

    @Test
    void searchTicketsReturnsEmptyPageWhenNoDocumentsMatch() {
        when(mongoTemplate.count(any(Query.class), eq(SearchTicketDocument.class), eq("tickets")))
                .thenReturn(Mono.just(0L));
        when(mongoTemplate.find(any(Query.class), eq(SearchTicketDocument.class), eq("tickets")))
                .thenReturn(Flux.empty());

        PageResponse<SearchTicketResponse> response = service
                .searchTickets(null, new SearchTicketRequest(), new PageRequestDto(0, 10))
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void indexTicketRequiresTicketIdBeforeSaving() {
        IndexTicketRequest request = new IndexTicketRequest(
                null, "No id", null, null, null, null, null, null, null, null, 0, List.of(), null, null, null
        );

        assertThatThrownBy(() -> service.indexTicket(request).block())
                .isInstanceOf(AppExceptions.BadRequestException.class);
    }

    @Test
    void indexTicketSavesMongoDocument() {
        IndexTicketRequest request = new IndexTicketRequest(
                TICKET_ID, "Rock Night", "SE1", "SGN", "Sai Gon", "DAD", "Da Nang", null, null, null, 0, List.of(), null, null, null
        );

        when(mongoTemplate.save(any(SearchTicketDocument.class), eq("tickets")))
                .thenReturn(Mono.just(SearchTicketDocument.builder().id(TICKET_ID).title("Rock Night").build()));

        service.indexTicket(request).block();

        ArgumentCaptor<SearchTicketDocument> documentCaptor = ArgumentCaptor.forClass(SearchTicketDocument.class);
        verify(mongoTemplate).save(documentCaptor.capture(), eq("tickets"));
        assertThat(documentCaptor.getValue().getId()).isEqualTo(TICKET_ID);
        assertThat(documentCaptor.getValue().getTitle()).isEqualTo("Rock Night");
    }

    @Test
    void deleteTicketRemovesMongoDocument() {
        when(mongoTemplate.remove(any(Query.class), eq(SearchTicketDocument.class), eq("tickets")))
                .thenReturn(Mono.just(DeleteResult.acknowledged(1L)));

        service.deleteTicket(TICKET_ID).block();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(queryCaptor.capture(), eq(SearchTicketDocument.class), eq("tickets"));
        assertThat(queryCaptor.getValue().getQueryObject()).containsKey("_id");
    }
}
