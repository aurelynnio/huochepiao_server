package codex.mmxxvi.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import codex.mmxxvi.integration.search.TicketSearchIndexClient;
import codex.mmxxvi.dto.integration.search.IndexTicketItemRequest;
import codex.mmxxvi.dto.integration.search.IndexTicketRequest;
import codex.mmxxvi.dto.internal.AdjustTicketStockRequest;
import codex.mmxxvi.dto.internal.TicketItemSnapshotResponse;
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
import codex.mmxxvi.service.TicketService;
import codex.mmxxvi.support.InternalApiKeyService;
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
    private final MongoTemplate mongoTemplate;
    private final InternalApiKeyService internalApiKeyService;

    public TicketServiceImpl(
            TicketRepository ticketRepository,
            TicketSearchIndexClient ticketSearchIndexClient,
            CachingService cachingService,
            MongoTemplate mongoTemplate,
            InternalApiKeyService internalApiKeyService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketSearchIndexClient = ticketSearchIndexClient;
        this.cachingService = cachingService;
        this.mongoTemplate = mongoTemplate;
        this.internalApiKeyService = internalApiKeyService;
    }

    private ResponseTicket toResponse(Ticket ticket) {
        return ResponseTicket.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .trainNumber(ticket.getTrainNumber())
                .departureStationCode(ticket.getDepartureStationCode())
                .departureStationName(ticket.getDepartureStationName())
                .arrivalStationCode(ticket.getArrivalStationCode())
                .arrivalStationName(ticket.getArrivalStationName())
                .journeyNote(ticket.getJourneyNote())
                .dateStart(ticket.getDateStart())
                .dateEnd(ticket.getDateEnd())
                .status(ticket.getStatus())
                .ticketItems(ticket.getTicketItems() == null
                        ? List.of()
                        : ticket.getTicketItems().stream().map(this::normalizeTicketItem).toList())
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
    public Mono<ResponseTicket> createTicket(CreateTicketRequest createTicketRequest) {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "ticket.write");
                            requireAdmin(authContext);

                            Ticket ticket = Ticket.builder()
                                    .title(createTicketRequest.getTitle().trim())
                                    .trainNumber(normalizeCode(createTicketRequest.getTrainNumber()))
                                    .departureStationCode(normalizeCode(createTicketRequest.getDepartureStationCode()))
                                    .departureStationName(normalizeText(createTicketRequest.getDepartureStationName()))
                                    .arrivalStationCode(normalizeCode(createTicketRequest.getArrivalStationCode()))
                                    .arrivalStationName(normalizeText(createTicketRequest.getArrivalStationName()))
                                    .journeyNote(normalizeText(createTicketRequest.getJourneyNote()))
                                    .dateStart(createTicketRequest.getDateStart())
                                    .dateEnd(createTicketRequest.getDateEnd())
                                    .status(0)
                                    .build();
                            ticket.setTicketItems(toTicketItems(ticket.getId(), createTicketRequest.getTicketItems()));

                            Ticket createdTicket = ticketRepository.save(ticket);
                            invalidateTicketCaches(createdTicket.getId());
                            syncTicketToSearch(createdTicket);
                            return toResponse(createdTicket);
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
                            if (updateTicketRequest.getTrainNumber() != null) {
                                oldTicket.setTrainNumber(normalizeCode(updateTicketRequest.getTrainNumber()));
                            }
                            if (updateTicketRequest.getDepartureStationCode() != null) {
                                oldTicket.setDepartureStationCode(normalizeCode(updateTicketRequest.getDepartureStationCode()));
                            }
                            if (updateTicketRequest.getDepartureStationName() != null) {
                                oldTicket.setDepartureStationName(normalizeText(updateTicketRequest.getDepartureStationName()));
                            }
                            if (updateTicketRequest.getArrivalStationCode() != null) {
                                oldTicket.setArrivalStationCode(normalizeCode(updateTicketRequest.getArrivalStationCode()));
                            }
                            if (updateTicketRequest.getArrivalStationName() != null) {
                                oldTicket.setArrivalStationName(normalizeText(updateTicketRequest.getArrivalStationName()));
                            }
                            if (updateTicketRequest.getJourneyNote() != null) {
                                oldTicket.setJourneyNote(normalizeText(updateTicketRequest.getJourneyNote()));
                            }

                            if (updateTicketRequest.getDateStart() != null) {
                                oldTicket.setDateStart(updateTicketRequest.getDateStart());
                            }
                            if (updateTicketRequest.getDateEnd() != null) {
                                oldTicket.setDateEnd(updateTicketRequest.getDateEnd());
                            }

                            var updatedTicket = ticketRepository.save(oldTicket);
                            invalidateTicketCaches(ticketId);
                            syncTicketToSearch(updatedTicket);
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
                    syncTicketToSearch(updatedTicket);
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
                        removeTicketFromSearch(ticketId);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .then();
        });
    }

    @Override
    public Mono<TicketItemSnapshotResponse> getTicketItemSnapshot(UUID ticketItemId) {
        return Mono.fromCallable(() -> findTicketItem(ticketItemId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<TicketItemSnapshotResponse> reserveTicketItem(UUID ticketItemId, AdjustTicketStockRequest request) {
        return Mono.fromCallable(() -> reserveTicketItemInternal(ticketItemId, request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<TicketItemSnapshotResponse> releaseTicketItem(UUID ticketItemId, AdjustTicketStockRequest request) {
        return Mono.fromCallable(() -> releaseTicketItemInternal(ticketItemId, request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void syncTicketToSearch(Ticket ticket) {
        try {
            ticketSearchIndexClient.indexTicket(internalApiKeyService.getInternalApiKey(), toIndexTicketRequest(ticket));
        } catch (Exception ex) {
            throw new AppExceptions.BadGatewayException("Failed to index ticket in search-service", ex);
        }
    }

    private void removeTicketFromSearch(UUID ticketId) {
        try {
            ticketSearchIndexClient.deleteTicket(internalApiKeyService.getInternalApiKey(), ticketId);
        } catch (Exception ex) {
            throw new AppExceptions.BadGatewayException("Failed to delete ticket from search-service", ex);
        }
    }

    private TicketItemSnapshotResponse reserveTicketItemInternal(UUID ticketItemId, AdjustTicketStockRequest request) {
        int quantity = request.getQuantity();
        TicketItemLookup lookup = findTicketItemLookup(ticketItemId);
        validateTicketItemCanBeReserved(lookup.item(), quantity);
        List<String> nextAvailableSeatLabels = new ArrayList<>(safeList(lookup.item().getAvailableSeatLabels()));
        List<String> reservedSeatLabels = new ArrayList<>(nextAvailableSeatLabels.subList(0, quantity));
        nextAvailableSeatLabels.subList(0, quantity).clear();

        Query query = Query.query(Criteria.where("ticketItems.id").is(ticketItemId)
                .and("ticketItems.stockAvailable").gte(quantity));
        Update update = new Update()
                .inc("ticketItems.$.stockAvailable", -quantity)
                .set("ticketItems.$.availableSeatLabels", nextAvailableSeatLabels);
        Ticket updatedTicket = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Ticket.class
        );

        if (updatedTicket == null) {
            throw new AppExceptions.ConflictException("Not enough seats available");
        }

        invalidateTicketCaches(updatedTicket.getId());
        syncTicketToSearch(updatedTicket);
        return toSnapshot(updatedTicket, findItemById(updatedTicket, ticketItemId), reservedSeatLabels);
    }

    private TicketItemSnapshotResponse releaseTicketItemInternal(UUID ticketItemId, AdjustTicketStockRequest request) {
        TicketItemLookup lookup = findTicketItemLookup(ticketItemId);
        List<String> masterSeatLabels = safeList(lookup.item().getSeatLabels());
        List<String> availableSeatLabels = new ArrayList<>(safeList(lookup.item().getAvailableSeatLabels()));
        List<String> seatLabelsToRelease = normalizeSeatLabels(request.getSeatLabels());
        int quantity = request.getQuantity();

        if (seatLabelsToRelease.isEmpty()) {
            for (String seatLabel : masterSeatLabels) {
                if (!availableSeatLabels.contains(seatLabel)) {
                    availableSeatLabels.add(seatLabel);
                }
                if (availableSeatLabels.size() >= valueOrZero(lookup.item().getStockAvailable()) + quantity) {
                    break;
                }
            }
        } else {
            for (String seatLabel : seatLabelsToRelease) {
                if (masterSeatLabels.contains(seatLabel) && !availableSeatLabels.contains(seatLabel)) {
                    availableSeatLabels.add(seatLabel);
                }
            }
        }

        List<String> normalizedAvailableSeatLabels = masterSeatLabels.stream()
                .filter(availableSeatLabels::contains)
                .toList();
        int nextAvailable = Math.min(valueOrZero(lookup.item().getStockInitial()), normalizedAvailableSeatLabels.size());

        Query query = Query.query(Criteria.where("ticketItems.id").is(ticketItemId));
        Update update = new Update()
                .set("ticketItems.$.stockAvailable", nextAvailable)
                .set("ticketItems.$.availableSeatLabels", normalizedAvailableSeatLabels);
        Ticket updatedTicket = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Ticket.class
        );

        if (updatedTicket == null) {
            throw new AppExceptions.ResourceNotFoundException("Ticket item not found");
        }

        invalidateTicketCaches(updatedTicket.getId());
        syncTicketToSearch(updatedTicket);
        return toSnapshot(updatedTicket, findItemById(updatedTicket, ticketItemId));
    }

    private void validateTicketItemCanBeReserved(TicketItem item, int quantity) {
        if (item == null) {
            throw new AppExceptions.ResourceNotFoundException("Ticket item not found");
        }

        if (!item.isStockPrepared()) {
            throw new AppExceptions.ConflictException("Ticket item is not prepared for sale");
        }

        if (!isWithinSaleWindow(item)) {
            throw new AppExceptions.ConflictException("Ticket item is outside the sale window");
        }

        if (valueOrZero(item.getStockAvailable()) < quantity) {
            throw new AppExceptions.ConflictException("Not enough seats available");
        }
    }

    private boolean isWithinSaleWindow(TicketItem item) {
        var now = java.time.LocalDateTime.now();
        if (item.getSaleStartTime() != null && now.isBefore(item.getSaleStartTime())) {
            return false;
        }
        if (item.getSaleEndTime() != null && now.isAfter(item.getSaleEndTime())) {
            return false;
        }
        return true;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private TicketItemSnapshotResponse findTicketItem(UUID ticketItemId) {
        TicketItemLookup lookup = findTicketItemLookup(ticketItemId);
        return toSnapshot(lookup.ticket(), lookup.item());
    }

    private TicketItemLookup findTicketItemLookup(UUID ticketItemId) {
        Ticket ticket = mongoTemplate.findOne(
                Query.query(Criteria.where("ticketItems.id").is(ticketItemId)),
                Ticket.class
        );

        if (ticket == null) {
            throw new AppExceptions.ResourceNotFoundException("Ticket item not found");
        }

        return new TicketItemLookup(ticket, findItemById(ticket, ticketItemId));
    }

    private TicketItem findItemById(Ticket ticket, UUID ticketItemId) {
        return ticket.getTicketItems().stream()
                .filter(item -> ticketItemId.equals(item.getId()))
                .map(this::normalizeTicketItem)
                .findFirst()
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Ticket item not found"));
    }

    private TicketItemSnapshotResponse toSnapshot(Ticket ticket, TicketItem item) {
        return toSnapshot(ticket, item, List.of());
    }

    private TicketItemSnapshotResponse toSnapshot(Ticket ticket, TicketItem item, List<String> reservedSeatLabels) {
        return TicketItemSnapshotResponse.builder()
                .ticketId(ticket.getId())
                .ticketItemId(item.getId())
                .ticketTitle(ticket.getTitle())
                .trainNumber(ticket.getTrainNumber())
                .departureStationCode(ticket.getDepartureStationCode())
                .departureStationName(ticket.getDepartureStationName())
                .arrivalStationCode(ticket.getArrivalStationCode())
                .arrivalStationName(ticket.getArrivalStationName())
                .ticketStatus(ticket.getStatus())
                .ticketDateStart(ticket.getDateStart())
                .ticketDateEnd(ticket.getDateEnd())
                .itemName(item.getName())
                .description(item.getDescription())
                .coachCode(item.getCoachCode())
                .seatClass(item.getSeatClass())
                .seatType(item.getSeatType())
                .seatLabels(item.getSeatLabels())
                .availableSeatLabels(item.getAvailableSeatLabels())
                .reservedSeatLabels(reservedSeatLabels)
                .stockInitial(item.getStockInitial())
                .stockAvailable(item.getStockAvailable())
                .stockPrepared(item.isStockPrepared())
                .priceOriginal(item.getPriceOriginal())
                .priceFlash(item.getPriceFlash())
                .saleStartTime(item.getSaleStartTime())
                .saleEndTime(item.getSaleEndTime())
                .build();
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
                ticket.getTrainNumber(),
                ticket.getDepartureStationCode(),
                ticket.getDepartureStationName(),
                ticket.getArrivalStationCode(),
                ticket.getArrivalStationName(),
                ticket.getJourneyNote(),
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
                        item.getCoachCode(),
                        item.getSeatClass(),
                        item.getSeatType(),
                        item.getSeatLabels(),
                        item.getAvailableSeatLabels(),
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
            ticketItems.add(normalizeTicketItem(TicketItem.builder()
                    .id(requestItem.getId() != null ? requestItem.getId() : UUID.randomUUID())
                    .ticketId(ticketId)
                    .name(name)
                    .description(description)
                    .coachCode(normalizeCode(requestItem.getCoachCode()))
                    .seatClass(normalizeText(requestItem.getSeatClass()))
                    .seatType(normalizeText(requestItem.getSeatType()))
                    .seatLabels(normalizeSeatLabels(requestItem.getSeatLabels()))
                    .stockInitial(requestItem.getStockInitial())
                    .stockAvailable(requestItem.getStockAvailable())
                    .stockPrepared(Boolean.TRUE.equals(requestItem.getStockPrepared()))
                    .priceOriginal(requestItem.getPriceOriginal())
                    .priceFlash(requestItem.getPriceFlash())
                    .saleStartTime(requestItem.getSaleStartTime())
                    .saleEndTime(requestItem.getSaleEndTime())
                    .build()));
        }

        return ticketItems;
    }

    private TicketItem normalizeTicketItem(TicketItem item) {
        item.setName(normalizeText(item.getName()));
        item.setDescription(normalizeText(item.getDescription()));
        item.setCoachCode(normalizeCode(item.getCoachCode()));
        item.setSeatClass(normalizeText(item.getSeatClass()));
        item.setSeatType(normalizeText(item.getSeatType()));

        List<String> seatLabels = normalizeSeatLabels(item.getSeatLabels());
        int requestedStockInitial = valueOrZero(item.getStockInitial());
        int requestedStockAvailable = item.getStockAvailable() == null ? requestedStockInitial : valueOrZero(item.getStockAvailable());
        int inferredSeatCount = Math.max(requestedStockInitial, requestedStockAvailable);

        if (seatLabels.isEmpty() && inferredSeatCount > 0) {
            seatLabels = buildSeatLabels(item.getCoachCode(), inferredSeatCount);
        }

        if (!seatLabels.isEmpty()) {
            inferredSeatCount = seatLabels.size();
        }

        int nextStockAvailable = Math.min(inferredSeatCount, requestedStockAvailable);
        List<String> availableSeatLabels = normalizeSeatLabels(item.getAvailableSeatLabels());
        if (availableSeatLabels.isEmpty() && !seatLabels.isEmpty()) {
            availableSeatLabels = new ArrayList<>(seatLabels.subList(0, Math.min(nextStockAvailable, seatLabels.size())));
        } else if (!seatLabels.isEmpty()) {
            List<String> requestedAvailableSeatLabels = availableSeatLabels;
            availableSeatLabels = seatLabels.stream()
                    .filter(requestedAvailableSeatLabels::contains)
                    .limit(nextStockAvailable)
                    .toList();
        }

        item.setSeatLabels(seatLabels);
        item.setAvailableSeatLabels(availableSeatLabels);
        item.setStockInitial(inferredSeatCount);
        item.setStockAvailable(availableSeatLabels.size());
        return item;
    }

    private List<String> buildSeatLabels(String coachCode, int seatCount) {
        if (seatCount <= 0) {
            return List.of();
        }

        String prefix = StringUtils.hasText(coachCode) ? coachCode : "SEAT";
        int width = seatCount >= 100 ? 3 : 2;
        List<String> seatLabels = new ArrayList<>(seatCount);
        for (int index = 1; index <= seatCount; index++) {
            seatLabels.add(prefix + "-" + String.format(Locale.ROOT, "%0" + width + "d", index));
        }
        return seatLabels;
    }

    private List<String> normalizeSeatLabels(List<String> seatLabels) {
        if (seatLabels == null || seatLabels.isEmpty()) {
            return List.of();
        }

        List<String> normalizedSeatLabels = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String seatLabel : seatLabels) {
            String normalizedSeatLabel = normalizeCode(seatLabel);
            if (StringUtils.hasText(normalizedSeatLabel) && seen.add(normalizedSeatLabel)) {
                normalizedSeatLabels.add(normalizedSeatLabel);
            }
        }
        return normalizedSeatLabels;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeCode(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
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
        return new AuthContext(userId, role, scopes);
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

    private record TicketItemLookup(Ticket ticket, TicketItem item) {
    }

    private record AuthContext(UUID userId, int role, Set<String> scopes) {
        boolean isAdmin() {
            return role >= ROLE_ADMIN;
        }
    }
}
