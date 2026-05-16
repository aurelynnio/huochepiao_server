package codex.mmxxvi.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import codex.mmxxvi.client.search.TicketSearchIndexClient;
import codex.mmxxvi.client.search.dto.IndexTicketItemRequest;
import codex.mmxxvi.client.search.dto.IndexTicketRequest;
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
import codex.mmxxvi.service.TicketService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class TicketServiceImpl implements TicketService {
    private static final int ROLE_ADMIN = 1;
    private static final String TICKET_CACHE_PREFIX = "ticket-service:tickets:";
    private static final String TICKET_LIST_CACHE_PREFIX = TICKET_CACHE_PREFIX + "pages:";
    private static final long TICKET_CACHE_TTL_MINUTES = 10L;

    private final TicketRepository ticketRepository;
    private final TicketSearchIndexClient ticketSearchIndexClient;
    private final CachingService cachingService;

    public TicketServiceImpl(
            TicketRepository ticketRepository,
            TicketSearchIndexClient ticketSearchIndexClient,
            CachingService cachingService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketSearchIndexClient = ticketSearchIndexClient;
        this.cachingService = cachingService;
    }

    private ResponseTicket toResponse(Ticket ticket) {
        return ResponseTicket.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .dateStart(ticket.getDateStart())
                .dateEnd(ticket.getDateEnd())
                .ticketItems(ticket.getTicketItems())
                .build();
    }

    @Override
    public Mono<PageResponse<ResponseTicket>> getTickets(PageRequestDto pageRequestDto) {
        return resolveAuthContext().flatMap(authContext ->
            Mono.fromCallable(() -> {
                    requireAnyScope(authContext, "ticket.read", "ticket.write");

                    String cacheKey = ticketListCacheKey(pageRequestDto);
                    PageResponse<ResponseTicket> cachedResponse = getCachedPage(cacheKey);
                    if (cachedResponse != null) {
                        return cachedResponse;
                    }

                    Pageable pageable = pageRequestDto.getPageable();
                    Page<Ticket> ticketPage = ticketRepository.findAll(pageable);
                    PageResponse<ResponseTicket> response = PageResponse.<ResponseTicket>builder()
                        .content(ticketPage.getContent().stream().map(this::toResponse).toList())
                        .pageNo(ticketPage.getNumber())
                        .pageSize(ticketPage.getSize())
                        .totalElements(ticketPage.getTotalElements())
                        .totalPages(ticketPage.getTotalPages())
                        .last(ticketPage.isLast())
                        .build();
                    cachingService.setObject(cacheKey, response, TICKET_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                    return response;
                })
                .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<ResponseTicket> getTicket(UUID ticketId) {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "ticket.read", "ticket.write");
                            String cacheKey = ticketCacheKey(ticketId);
                            ResponseTicket cachedTicket = cachingService.getObject(cacheKey, ResponseTicket.class);
                            if (cachedTicket != null) {
                                return cachedTicket;
                            }

                            ResponseTicket ticket = ticketRepository.findTicketById(ticketId);
                            if (ticket != null) {
                                cachingService.setObject(cacheKey, ticket, TICKET_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                            }
                            return ticket;
                        })
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<ResponseTicket> createTicket() {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "ticket.write");
                            requireAdmin(authContext);

                            Ticket ticket = new Ticket();
                            ticket.setId(UUID.randomUUID());
                            return toResponse(ticket);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<ResponseTicket> updateTicket(UUID ticketId, UpdateTicketRequest updateTicketRequest) {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "ticket.write");
                            requireAdmin(authContext);

                            var oldTicket = ticketRepository.findById(ticketId)
                                    .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Ticket not found"));

                            if (updateTicketRequest.getTitle() != null) {
                                oldTicket.setTitle(updateTicketRequest.getTitle().trim());
                            }

                            if (updateTicketRequest.getDateStart() != null) {
                                oldTicket.setDateStart(updateTicketRequest.getDateStart());
                            }
                            if (updateTicketRequest.getDateEnd() != null) {
                                oldTicket.setDateEnd(updateTicketRequest.getDateEnd());
                            }

                            var updatedTicket = ticketRepository.save(oldTicket);
                            invalidateTicketCaches(ticketId);
                            syncTicketToSearch(updatedTicket, authContext);
                            return toResponse(updatedTicket);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<ResponseTicket> updateTicketItems(UUID ticketId, UpdateTicketItemsRequest updateTicketItemsRequest) {
        return resolveAuthContext().flatMap(authContext ->
            Mono.fromCallable(() -> {
                    requireAnyScope(authContext, "ticket.write");
                    requireAdmin(authContext);

                    var ticket = ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Ticket not found"));

                    ticket.setTicketItems(toTicketItems(ticketId, updateTicketItemsRequest.getTicketItems()));
                    var updatedTicket = ticketRepository.save(ticket);
                    invalidateTicketCaches(ticketId);
                    syncTicketToSearch(updatedTicket, authContext);
                    return toResponse(updatedTicket);
                })
                .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<Void> deleteTicket(UUID ticketId) {
        return resolveAuthContext().flatMap(authContext -> {
            requireAnyScope(authContext, "ticket.write");
            requireAdmin(authContext);

            return Mono.fromRunnable(() -> {
                        ticketRepository.deleteById(ticketId);
                        invalidateTicketCaches(ticketId);
                        removeTicketFromSearch(ticketId, authContext);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .then();
        });
    }

    private void syncTicketToSearch(Ticket ticket, AuthContext authContext) {
        try {
            ticketSearchIndexClient.indexTicket(authContext.authorizationHeader(), toIndexTicketRequest(ticket));
        } catch (Exception ex) {
            throw new AppExceptions.BadGatewayException("Failed to index ticket in search-service", ex);
        }
    }

    private void removeTicketFromSearch(UUID ticketId, AuthContext authContext) {
        try {
            ticketSearchIndexClient.deleteTicket(authContext.authorizationHeader(), ticketId);
        } catch (Exception ex) {
            throw new AppExceptions.BadGatewayException("Failed to delete ticket from search-service", ex);
        }
    }

    private String ticketCacheKey(UUID ticketId) {
        return TICKET_CACHE_PREFIX + ticketId;
    }

    private String ticketListCacheKey(PageRequestDto pageRequestDto) {
        return TICKET_LIST_CACHE_PREFIX
                + pageRequestDto.getPageNo()
                + ":"
                + pageRequestDto.getPageSize()
                + ":"
                + pageRequestDto.getSortBy()
                + ":"
                + pageRequestDto.getSortDir().toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private PageResponse<ResponseTicket> getCachedPage(String cacheKey) {
        return cachingService.getObject(cacheKey, PageResponse.class);
    }

    private void invalidateTicketCaches(UUID ticketId) {
        cachingService.delete(ticketCacheKey(ticketId));
        cachingService.deleteByPattern(TICKET_LIST_CACHE_PREFIX + "*");
    }

    private IndexTicketRequest toIndexTicketRequest(Ticket ticket) {
        return new IndexTicketRequest(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDateStart(),
                ticket.getDateEnd(),
                ticket.getStatus(),
                toIndexTicketItemRequests(ticket.getTicketItems()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getDeletedAt()
        );
    }

    private List<IndexTicketItemRequest> toIndexTicketItemRequests(List<TicketItem> ticketItems) {
        if (ticketItems == null) {
            return List.of();
        }

        return ticketItems.stream()
                .map(item -> new IndexTicketItemRequest(
                        item.getId(),
                        item.getTicketId(),
                        item.getName(),
                        item.getDescription(),
                        item.getStockInitial(),
                        item.getStockAvailable(),
                        item.isStockPrepared(),
                        item.getPriceOriginal(),
                        item.getPriceFlash(),
                        item.getSaleStartTime(),
                        item.getSaleEndTime(),
                        item.getCreatedAt(),
                        item.getUpdatedAt(),
                        item.getDeletedAt()
                ))
                .toList();
    }

    private List<TicketItem> toTicketItems(UUID ticketId, List<UpdateTicketItemRequest> requestItems) {
        if (requestItems == null) {
            return List.of();
        }

        List<TicketItem> ticketItems = new ArrayList<>(requestItems.size());
        for (UpdateTicketItemRequest requestItem : requestItems) {
            if (requestItem == null) {
                continue;
            }

            String name = requestItem.getName() == null ? null : requestItem.getName().trim();
            String description = requestItem.getDescription() == null ? null : requestItem.getDescription().trim();

            ticketItems.add(TicketItem.builder()
                    .id(requestItem.getId() != null ? requestItem.getId() : UUID.randomUUID())
                    .ticketId(ticketId)
                    .name(name)
                    .description(description)
                    .stockInitial(requestItem.getStockInitial())
                    .stockAvailable(requestItem.getStockAvailable())
                    .stockPrepared(Boolean.TRUE.equals(requestItem.getStockPrepared()))
                    .priceOriginal(requestItem.getPriceOriginal())
                    .priceFlash(requestItem.getPriceFlash())
                    .saleStartTime(requestItem.getSaleStartTime())
                    .saleEndTime(requestItem.getSaleEndTime())
                    .build());
        }

        return ticketItems;
    }

    private Mono<AuthContext> resolveAuthContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast)
                .map(this::toAuthContext)
                .switchIfEmpty(Mono.error(new AppExceptions.UnauthorizedException("Unauthorized")));
    }

    private AuthContext toAuthContext(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenantId");
        if (!StringUtils.hasText(tenantId)) {
            throw new AppExceptions.ForbiddenException("Token tenantId claim is missing");
        }

        Object userIdClaim = jwt.getClaims().get("userId");
        if (userIdClaim == null) {
            throw new AppExceptions.ForbiddenException("Token userId claim is missing");
        }

        UUID userId;
        try {
            userId = UUID.fromString(String.valueOf(userIdClaim));
        } catch (IllegalArgumentException ex) {
            throw new AppExceptions.ForbiddenException("Token userId claim is invalid");
        }

        Object roleClaim = jwt.getClaims().get("role");
        int role;
        if (roleClaim instanceof Number number) {
            role = number.intValue();
        } else {
            try {
                role = Integer.parseInt(String.valueOf(roleClaim));
            } catch (Exception ex) {
                throw new AppExceptions.ForbiddenException("Token role claim is invalid");
            }
        }

        Set<String> scopes = parseScopes(jwt.getClaims().get("scope"));
        return new AuthContext(userId, role, scopes, jwt.getTokenValue());
    }

    private Set<String> parseScopes(Object scopeClaim) {
        if (scopeClaim == null) {
            return Collections.emptySet();
        }

        if (scopeClaim instanceof String scopeText) {
            if (!StringUtils.hasText(scopeText)) {
                return Collections.emptySet();
            }
            return Arrays.stream(scopeText.trim().split("\\s+"))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        if (scopeClaim instanceof Iterable<?> iterable) {
            Set<String> scopes = new LinkedHashSet<>();
            for (Object value : iterable) {
                if (value != null && StringUtils.hasText(String.valueOf(value))) {
                    scopes.add(String.valueOf(value));
                }
            }
            return scopes;
        }

        return Collections.emptySet();
    }

    private void requireAnyScope(AuthContext authContext, String... expectedScopes) {
        for (String expectedScope : expectedScopes) {
            if (authContext.scopes().contains(expectedScope)) {
                return;
            }
        }
        throw new AppExceptions.ForbiddenException("Insufficient scope");
    }

    private void requireAdmin(AuthContext authContext) {
        if (!authContext.isAdmin()) {
            throw new AppExceptions.ForbiddenException("Admin role is required");
        }
    }

    private record AuthContext(UUID userId, int role, Set<String> scopes, String tokenValue) {
        boolean isAdmin() {
            return role >= ROLE_ADMIN;
        }

        String authorizationHeader() {
            return "Bearer " + tokenValue;
        }
    }
}
