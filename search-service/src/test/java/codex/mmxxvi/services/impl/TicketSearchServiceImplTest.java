package codex.mmxxvi.services.impl;

import codex.mmxxvi.dto.request.IndexTicketRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.SearchTicketResponse;
import codex.mmxxvi.exception.AppExceptions;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketSearchServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TICKET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private HttpServer server;
    private CapturedRequest capturedRequest;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void searchTicketsMapsElasticsearchHitsToPageResponse() {
        respond(200, """
                {
                  "hits": {
                    "total": {"value": 1},
                    "hits": [
                      {
                        "_score": 2.5,
                        "_source": {
                          "id": "22222222-2222-2222-2222-222222222222",
                          "title": "Rock Night",
                          "status": 0,
                          "ticketItems": []
                        }
                      }
                    ]
                  }
                }
                """);
        TicketSearchServiceImpl service = service();

        PageResponse<SearchTicketResponse> response = service
                .searchTickets("rock", new PageRequestDto(0, 10))
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getId()).isEqualTo(TICKET_ID);
        assertThat(response.getContent().getFirst().getScore()).isEqualTo(2.5);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(capturedRequest.method()).isEqualTo("POST");
        assertThat(capturedRequest.uri()).isEqualTo("/tickets/_search");
        assertThat(capturedRequest.body()).contains("\"operator\":\"and\"");
    }

    @Test
    void searchTicketsReturnsEmptyPageWhenIndexDoesNotExist() {
        respond(404, "{\"error\":\"not found\"}");
        TicketSearchServiceImpl service = service();

        PageResponse<SearchTicketResponse> response = service
                .searchTickets(null, new PageRequestDto(0, 10))
                .block();

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void indexTicketRequiresTicketIdBeforeCallingElasticsearch() {
        TicketSearchServiceImpl service = service();
        IndexTicketRequest request = new IndexTicketRequest(null, "No id", null, null, 0, List.of(), null, null, null);

        assertThatThrownBy(() -> withAuth(service.indexTicket(request), USER_ID, 1, "search.index"))
                .isInstanceOf(AppExceptions.BadRequestException.class);
    }

    @Test
    void indexTicketSendsPutRequestWhenAdminHasSearchIndexScope() {
        respond(200, "{}");
        TicketSearchServiceImpl service = service();
        IndexTicketRequest request = new IndexTicketRequest(TICKET_ID, "Rock Night", null, null, 0, List.of(), null, null, null);

        withAuth(service.indexTicket(request), USER_ID, 1, "search.index");

        assertThat(capturedRequest.method()).isEqualTo("PUT");
        assertThat(capturedRequest.uri()).isEqualTo("/tickets/_doc/" + TICKET_ID + "?refresh=wait_for");
        assertThat(capturedRequest.body()).contains("\"title\":\"Rock Night\"");
    }

    @Test
    void deleteTicketIgnoresElasticsearchNotFound() {
        respond(404, "{\"error\":\"not found\"}");
        TicketSearchServiceImpl service = service();

        withAuth(service.deleteTicket(TICKET_ID), USER_ID, 1, "search.index");

        assertThat(capturedRequest.method()).isEqualTo("DELETE");
        assertThat(capturedRequest.uri()).isEqualTo("/tickets/_doc/" + TICKET_ID + "?refresh=wait_for");
    }

    @Test
    void indexTicketRequiresAdminRole() {
        TicketSearchServiceImpl service = service();
        IndexTicketRequest request = new IndexTicketRequest(TICKET_ID, "Rock Night", null, null, 0, List.of(), null, null, null);

        assertThatThrownBy(() -> withAuth(service.indexTicket(request), USER_ID, 0, "search.index"))
                .isInstanceOf(AppExceptions.ForbiddenException.class);
    }

    private TicketSearchServiceImpl service() {
        return new TicketSearchServiceImpl(new ObjectMapper(), "http://localhost:" + server.getAddress().getPort(), "tickets");
    }

    private void respond(int status, String body) {
        server.createContext("/", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            capturedRequest = new CapturedRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString(), requestBody);

            byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
    }

    private <T> T withAuth(Mono<T> mono, UUID userId, int role, String scopes) {
        return mono.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication(userId, role, scopes)))
                .block();
    }

    private Authentication authentication(UUID userId, int role, String scopes) {
        Jwt jwt = Jwt.withTokenValue("test-token")
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

    private record CapturedRequest(String method, String uri, String body) {
    }
}
