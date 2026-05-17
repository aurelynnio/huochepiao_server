package codex.mmxxvi.service.impl;

import codex.mmxxvi.client.search.TicketSearchIndexClient;
import codex.mmxxvi.dto.request.CreateTicketRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.UpdateTicketItemRequest;
import codex.mmxxvi.dto.request.UpdateTicketItemsRequest;
import codex.mmxxvi.dto.request.UpdateTicketRequest;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.ResponseTicket;
import codex.mmxxvi.entity.Ticket;
import codex.mmxxvi.entity.TicketItem;
import codex.mmxxvi.exception.AppExceptions;
import codex.mmxxvi.repository.TicketRepository;
import codex.mmxxvi.service.CachingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TICKET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ITEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketSearchIndexClient ticketSearchIndexClient;

    @Mock
    private CachingService cachingService;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketRepository, ticketSearchIndexClient, cachingService);
    }

    @Test
    void getTicketsReturnsPagedTicketsForReadScope() {
        Ticket ticket = ticket("Concert");
        when(ticketRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(ticket)));

        PageResponse<ResponseTicket> response = withAuth(ticketService.getTickets(new PageRequestDto()), USER_ID, 0, "ticket.read");

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getTitle()).isEqualTo("Concert");
        assertThat(response.getContent().getFirst().getStatus()).isEqualTo(0);
    }

    @Test
    void getTicketReturnsRepositoryProjection() {
        ResponseTicket projection = ResponseTicket.builder()
                .id(TICKET_ID)
                .title("Concert")
                .build();
        when(ticketRepository.findTicketById(TICKET_ID)).thenReturn(projection);

        ResponseTicket response = withAuth(ticketService.getTicket(TICKET_ID), USER_ID, 0, "ticket.read");

        assertThat(response).isEqualTo(projection);
    }

    @Test
    void getTicketReturnsCachedTicketWithoutQueryingRepository() {
        ResponseTicket cachedTicket = ResponseTicket.builder()
                .id(TICKET_ID)
                .title("Cached concert")
                .build();
        when(cachingService.getObject("ticket-service:tickets:" + TICKET_ID, ResponseTicket.class))
                .thenReturn(cachedTicket);

        ResponseTicket response = withAuth(ticketService.getTicket(TICKET_ID), USER_ID, 0, "ticket.read");

        assertThat(response).isEqualTo(cachedTicket);
        verify(ticketRepository, never()).findTicketById(TICKET_ID);
    }

    @Test
    void createTicketPersistsTicketAndSyncsSearchIndex() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("  Sai Gon - Nha Trang  ")
                .dateStart(LocalDateTime.of(2026, 5, 20, 22, 0))
                .dateEnd(LocalDateTime.of(2026, 5, 21, 6, 30))
                .ticketItems(List.of(UpdateTicketItemRequest.builder()
                        .name("  Soft Seat  ")
                        .description("  Coach C  ")
                        .stockInitial(40)
                        .stockAvailable(40)
                        .stockPrepared(true)
                        .priceOriginal(450_000L)
                        .priceFlash(390_000L)
                        .build()))
                .build();

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseTicket response = withAuth(ticketService.createTicket(request), USER_ID, 1, "ticket.write");

        assertThat(response.getTitle()).isEqualTo("Sai Gon - Nha Trang");
        assertThat(response.getStatus()).isEqualTo(0);
        assertThat(response.getTicketItems()).hasSize(1);
        assertThat(response.getTicketItems().getFirst().getTicketId()).isEqualTo(response.getId());
        assertThat(response.getTicketItems().getFirst().getName()).isEqualTo("Soft Seat");
        verify(ticketSearchIndexClient).indexTicket(eq("Bearer ticket-token"), any());
        verify(cachingService).delete("ticket-service:tickets:" + response.getId());
        verify(cachingService).deleteByPattern("ticket-service:tickets:pages:*");
    }

    @Test
    void updateTicketRequiresAdminRoleAndSyncsSearchIndex() {
        Ticket ticket = ticket("Old title");
        UpdateTicketRequest request = UpdateTicketRequest.builder()
                .title("  New title  ")
                .dateStart(LocalDateTime.of(2026, 5, 15, 9, 0))
                .build();

        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseTicket response = withAuth(ticketService.updateTicket(TICKET_ID, request), USER_ID, 1, "ticket.write");

        assertThat(response.getTitle()).isEqualTo("New title");

        ArgumentCaptor<codex.mmxxvi.client.search.dto.IndexTicketRequest> requestCaptor =
                ArgumentCaptor.forClass(codex.mmxxvi.client.search.dto.IndexTicketRequest.class);
        verify(ticketSearchIndexClient).indexTicket(eq("Bearer ticket-token"), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getId()).isEqualTo(TICKET_ID);
        assertThat(requestCaptor.getValue().getTitle()).isEqualTo("New title");
        verify(cachingService).delete("ticket-service:tickets:" + TICKET_ID);
        verify(cachingService).deleteByPattern("ticket-service:tickets:pages:*");
    }

    @Test
    void updateTicketItemsTrimsValuesAndDefaultsStockPreparedToFalse() {
        Ticket ticket = ticket("Concert");
        UpdateTicketItemsRequest request = UpdateTicketItemsRequest.builder()
                .ticketItems(List.of(UpdateTicketItemRequest.builder()
                        .id(ITEM_ID)
                        .name("  VIP  ")
                        .description("  Front row  ")
                        .stockInitial(10)
                        .stockAvailable(8)
                        .stockPrepared(null)
                        .priceOriginal(100_000L)
                        .build()))
                .build();

        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseTicket response = withAuth(ticketService.updateTicketItems(TICKET_ID, request), USER_ID, 1, "ticket.write");

        TicketItem item = response.getTicketItems().getFirst();
        assertThat(item.getName()).isEqualTo("VIP");
        assertThat(item.getDescription()).isEqualTo("Front row");
        assertThat(item.isStockPrepared()).isFalse();
        assertThat(item.getTicketId()).isEqualTo(TICKET_ID);
    }

    @Test
    void updateTicketMapsSearchClientFailureToBadGateway() {
        Ticket ticket = ticket("Concert");
        when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("search down")).when(ticketSearchIndexClient).indexTicket(any(), any());

        assertThatThrownBy(() -> withAuth(
                ticketService.updateTicket(TICKET_ID, UpdateTicketRequest.builder().title("Updated").build()),
                USER_ID,
                1,
                "ticket.write"
        )).isInstanceOf(AppExceptions.BadGatewayException.class);
    }

    @Test
    void deleteTicketDeletesRepositoryAndSearchIndex() {
        withAuth(ticketService.deleteTicket(TICKET_ID), USER_ID, 1, "ticket.write");

        verify(ticketRepository).deleteById(TICKET_ID);
        verify(ticketSearchIndexClient).deleteTicket("Bearer ticket-token", TICKET_ID);
    }

    private Ticket ticket(String title) {
        return Ticket.builder()
                .id(TICKET_ID)
                .title(title)
                .ticketItems(List.of(TicketItem.builder()
                        .id(ITEM_ID)
                        .ticketId(TICKET_ID)
                        .name("General")
                        .priceOriginal(50_000L)
                        .build()))
                .build();
    }

    private <T> T withAuth(Mono<T> mono, UUID userId, int role, String scopes) {
        return mono.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication(userId, role, scopes)))
                .block();
    }

    private Authentication authentication(UUID userId, int role, String scopes) {
        Jwt jwt = Jwt.withTokenValue("ticket-token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claims(claims -> claims.putAll(Map.of(
                        "tenantId", "public",
                        "userId", userId.toString(),
                        "role", role,
                        "scope", scopes,
                        "type", "access"
                )))
                .build();
        return new JwtAuthenticationToken(jwt, List.of());
    }
}
