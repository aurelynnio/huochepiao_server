package codex.mmxxvi.services.impl;

import codex.mmxxvi.integration.ticket.TicketInventoryClient;
import codex.mmxxvi.dto.integration.ticket.AdjustTicketStockRequest;
import codex.mmxxvi.dto.integration.ticket.TicketItemSnapshotResponse;
import codex.mmxxvi.dto.request.CreateOrderPassengerRequest;
import codex.mmxxvi.dto.request.CreateOrderRequest;
import codex.mmxxvi.dto.internal.InternalOrderResponse;
import codex.mmxxvi.repository.OrderRepository;
import codex.mmxxvi.exception.AppExceptions;
import codex.mmxxvi.support.InternalApiKeyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.response.OrderPassengerResponse;
import codex.mmxxvi.dto.response.OrderResponse;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.entity.Order;
import codex.mmxxvi.entity.OrderPassenger;
import codex.mmxxvi.services.OrderService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private static final int ROLE_ADMIN = 1;
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_COMPLETED = 1;
    private static final int STATUS_FAILED = 2;
    private static final int STATUS_REFUNDED = 3;
    private static final int STATUS_CANCELLED = 4;


    private final OrderRepository orderRepository;
    private final TicketInventoryClient ticketInventoryClient;
    private final InternalApiKeyService internalApiKeyService;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            TicketInventoryClient ticketInventoryClient,
            InternalApiKeyService internalApiKeyService
    ) {
        this.orderRepository = orderRepository;
        this.ticketInventoryClient = ticketInventoryClient;
        this.internalApiKeyService = internalApiKeyService;
    }

    private OrderResponse convertDTO(Order order){
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .ticketId(order.getTicketId())
                .ticketItemId(order.getTicketItemId())
                .ticketTitle(order.getTicketTitle())
                .trainNumber(order.getTrainNumber())
                .departureStationCode(order.getDepartureStationCode())
                .departureStationName(order.getDepartureStationName())
                .arrivalStationCode(order.getArrivalStationCode())
                .arrivalStationName(order.getArrivalStationName())
                .departureTime(order.getDepartureTime())
                .arrivalTime(order.getArrivalTime())
                .coachCode(order.getCoachCode())
                .seatClass(order.getSeatClass())
                .seatType(order.getSeatType())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalPrice(order.getTotalPrice())
                .ticketCode(order.getTicketCode())
                .qrPayload(order.getQrPayload())
                .seatLabels(order.getSeatLabels() == null ? List.of() : List.copyOf(order.getSeatLabels()))
                .passengers(toPassengerResponses(order.getPassengers()))
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private InternalOrderResponse convertInternal(Order order) {
        return InternalOrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .ticketItemId(order.getTicketItemId())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalPrice(order.getTotalPrice())
                .ticketCode(order.getTicketCode())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }


    @Override
    public Mono<PageResponse<OrderResponse>> getAllOrders(PageRequestDto pageRequestDto) {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "order.read", "order.read.self", "order.admin");
                            Pageable pageable = pageRequestDto.getPageable();

                            Page<Order> orderPage;
                            if (authContext.isAdmin() || authContext.scopes().contains("order.read")) {
                                orderPage = orderRepository.findAll(pageable);
                            } else {
                                orderPage = orderRepository.findByUserId(authContext.userId(), pageable);
                            }
                            return buildPageResponse(orderPage);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    private PageResponse<OrderResponse> buildPageResponse(Page<Order> orderPage) {
        return PageResponse.<OrderResponse>builder()
                .content(orderPage.getContent().stream()
                        .map(this::convertDTO)
                        .toList())
                .pageNo(orderPage.getNumber())
                .pageSize(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPage(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    @Override
    public Mono<OrderResponse> createOrder(CreateOrderRequest request) {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "order.write", "order.write.self", "order.admin");

                            if (!authContext.isAdmin() && !authContext.userId().equals(request.getUserId())) {
                                throw new AppExceptions.ForbiddenException("You cannot create orders for another user");
                            }

                            TicketItemSnapshotResponse ticketItem = reserveInventory(request.getTicketItemId(), request.getQuantity());
                            long unitPrice = resolveUnitPrice(ticketItem);
                            long totalPrice = Math.multiplyExact(unitPrice, request.getQuantity());
                            List<OrderPassenger> passengers = toPassengers(request.getPassengers(), request.getQuantity());

                            Order order = Order.builder()
                                    .userId(request.getUserId())
                                    .ticketId(ticketItem.getTicketId())
                                    .ticketItemId(ticketItem.getTicketItemId())
                                    .ticketTitle(ticketItem.getTicketTitle())
                                    .trainNumber(ticketItem.getTrainNumber())
                                    .departureStationCode(ticketItem.getDepartureStationCode())
                                    .departureStationName(ticketItem.getDepartureStationName())
                                    .arrivalStationCode(ticketItem.getArrivalStationCode())
                                    .arrivalStationName(ticketItem.getArrivalStationName())
                                    .departureTime(ticketItem.getTicketDateStart())
                                    .arrivalTime(ticketItem.getTicketDateEnd())
                                    .coachCode(ticketItem.getCoachCode())
                                    .seatClass(ticketItem.getSeatClass())
                                    .seatType(ticketItem.getSeatType())
                                    .seatLabels(ticketItem.getReservedSeatLabels() == null
                                            ? List.of()
                                            : List.copyOf(ticketItem.getReservedSeatLabels()))
                                    .passengers(passengers)
                                    .quantity(request.getQuantity())
                                    .unitPrice(unitPrice)
                                    .totalPrice(totalPrice)
                                    .status(STATUS_PENDING)
                                    .build();
                            try {
                                Order savedOrder = orderRepository.save(order);
                                savedOrder.setQrPayload(buildQrPayload(savedOrder));
                                return convertDTO(orderRepository.save(savedOrder));
                            } catch (RuntimeException ex) {
                                releaseInventory(ticketItem.getTicketItemId(), request.getQuantity(), ticketItem.getReservedSeatLabels());
                                throw ex;
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<OrderResponse> updateStatus(UUID id, Integer status) {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "order.admin");
                            requireAdmin(authContext);

                            return convertDTO(updateStatusInternalSync(id, status));
                        })
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<OrderResponse> cancelOrder(UUID id) {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "order.write", "order.write.self", "order.admin");

                            Order order = orderRepository.findById(id)
                                    .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Order not found"));

                            if (!authContext.isAdmin() && !authContext.userId().equals(order.getUserId())) {
                                throw new AppExceptions.ForbiddenException("You cannot cancel another user's order");
                            }

                            return convertDTO(updateStatusInternalSync(order, STATUS_CANCELLED));
                        })
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<PageResponse<OrderResponse>> filterOrderFollowingStatus(Integer status, PageRequestDto pageRequestDto) {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "order.read", "order.read.self", "order.admin");
                            Pageable pageable = pageRequestDto.getPageable();

                            Page<Order> orderPage;
                            if (authContext.isAdmin() || authContext.scopes().contains("order.read")) {
                                orderPage = orderRepository.findByStatus(status, pageable);
                            } else {
                                orderPage = orderRepository.findByUserIdAndStatus(authContext.userId(), status, pageable);
                            }
                            return buildPageResponse(orderPage);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<InternalOrderResponse> getOrderInternal(UUID id) {
        return Mono.fromCallable(() -> convertInternal(findOrder(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<OrderResponse> updateStatusInternal(UUID id, Integer status) {
        return Mono.fromCallable(() -> convertDTO(updateStatusInternalSync(id, status)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Order updateStatusInternalSync(UUID id, Integer status) {
        return updateStatusInternalSync(findOrder(id), status);
    }

    private Order updateStatusInternalSync(Order order, Integer targetStatus) {
        int normalizedTargetStatus = normalizeTargetStatus(targetStatus);
        int currentStatus = order.getStatus() == null ? STATUS_PENDING : order.getStatus();

        if (currentStatus == normalizedTargetStatus) {
            return order;
        }

        validateStatusTransition(currentStatus, normalizedTargetStatus);

        if (shouldReleaseInventory(currentStatus, normalizedTargetStatus)) {
            releaseInventory(order.getTicketItemId(), order.getQuantity(), order.getSeatLabels());
        }

        if (normalizedTargetStatus == STATUS_COMPLETED && !StringUtils.hasText(order.getTicketCode())) {
            order.setTicketCode(generateTicketCode(order));
            order.setQrPayload(buildQrPayload(order));
        }

        order.setStatus(normalizedTargetStatus);
        return orderRepository.save(order);
    }

    private void validateStatusTransition(int currentStatus, int targetStatus) {
        if (currentStatus == STATUS_PENDING
                && (targetStatus == STATUS_COMPLETED || targetStatus == STATUS_FAILED || targetStatus == STATUS_CANCELLED)) {
            return;
        }

        if (currentStatus == STATUS_COMPLETED && targetStatus == STATUS_REFUNDED) {
            return;
        }

        throw new AppExceptions.ConflictException("Unsupported order status transition");
    }

    private boolean shouldReleaseInventory(int currentStatus, int targetStatus) {
        return (currentStatus == STATUS_PENDING && (targetStatus == STATUS_FAILED || targetStatus == STATUS_CANCELLED))
                || (currentStatus == STATUS_COMPLETED && targetStatus == STATUS_REFUNDED);
    }

    private int normalizeTargetStatus(Integer status) {
        if (status == null) {
            throw new AppExceptions.BadRequestException("Status is required");
        }

        if (status < STATUS_PENDING || status > STATUS_CANCELLED) {
            throw new AppExceptions.BadRequestException("Unsupported order status");
        }

        return status;
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Order not found"));
    }

    private TicketItemSnapshotResponse reserveInventory(UUID ticketItemId, Integer quantity) {
        try {
            return ticketInventoryClient.reserveTicketItem(
                    internalApiKeyService.getInternalApiKey(),
                    ticketItemId,
                    AdjustTicketStockRequest.builder().quantity(quantity).build()
            );
        } catch (AppExceptions.ResourceNotFoundException | AppExceptions.ConflictException | AppExceptions.BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppExceptions.BadGatewayException("Failed to reserve ticket inventory", ex);
        }
    }

    private void releaseInventory(UUID ticketItemId, Integer quantity, List<String> seatLabels) {
        try {
            ticketInventoryClient.releaseTicketItem(
                    internalApiKeyService.getInternalApiKey(),
                    ticketItemId,
                    AdjustTicketStockRequest.builder()
                            .quantity(quantity)
                            .seatLabels(seatLabels == null ? List.of() : List.copyOf(seatLabels))
                            .build()
            );
        } catch (Exception ex) {
            throw new AppExceptions.BadGatewayException("Failed to release ticket inventory", ex);
        }
    }

    private long resolveUnitPrice(TicketItemSnapshotResponse ticketItem) {
        Long flashPrice = ticketItem.getPriceFlash();
        if (flashPrice != null && flashPrice > 0) {
            return flashPrice;
        }

        Long originalPrice = ticketItem.getPriceOriginal();
        if (originalPrice != null && originalPrice >= 0) {
            return originalPrice;
        }

        throw new AppExceptions.ConflictException("Ticket item price is invalid");
    }

    private String generateTicketCode(Order order) {
        String baseId = order.getId() == null
                ? UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase()
                : order.getId().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "VTR-" + baseId;
    }

    private String buildQrPayload(Order order) {
        List<String> seatLabels = order.getSeatLabels() == null ? List.of() : order.getSeatLabels();
        List<OrderPassenger> passengers = order.getPassengers() == null ? List.of() : order.getPassengers();
        return String.join("|",
                "ticketCode=" + (StringUtils.hasText(order.getTicketCode()) ? order.getTicketCode() : "PENDING"),
                "orderId=" + order.getId(),
                "train=" + defaultText(order.getTrainNumber()),
                "route=" + defaultText(order.getDepartureStationName()) + "-" + defaultText(order.getArrivalStationName()),
                "time=" + order.getDepartureTime(),
                "coach=" + defaultText(order.getCoachCode()),
                "seats=" + String.join(",", seatLabels),
                "passengers=" + passengers.stream()
                        .map(OrderPassenger::getFullName)
                        .collect(Collectors.joining(",")),
                "status=" + order.getStatus()
        );
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "N/A";
    }

    private List<OrderPassengerResponse> toPassengerResponses(List<OrderPassenger> passengers) {
        if (passengers == null || passengers.isEmpty()) {
            return List.of();
        }

        return passengers.stream()
                .map(passenger -> OrderPassengerResponse.builder()
                        .fullName(passenger.getFullName())
                        .passengerType(passenger.getPassengerType())
                        .identityNumber(passenger.getIdentityNumber())
                        .phoneNumber(passenger.getPhoneNumber())
                        .build())
                .toList();
    }

    private List<OrderPassenger> toPassengers(List<CreateOrderPassengerRequest> passengerRequests, int quantity) {
        if (passengerRequests == null || passengerRequests.isEmpty()) {
            List<OrderPassenger> placeholders = new ArrayList<>(quantity);
            for (int index = 1; index <= quantity; index++) {
                placeholders.add(OrderPassenger.builder()
                        .fullName("Hanh khach " + index)
                        .passengerType("ADULT")
                        .build());
            }
            return placeholders;
        }

        if (passengerRequests.size() != quantity) {
            throw new AppExceptions.BadRequestException("Passenger count must match order quantity");
        }

        return passengerRequests.stream()
                .map(this::toPassenger)
                .toList();
    }

    private OrderPassenger toPassenger(CreateOrderPassengerRequest request) {
        String fullName = normalizeText(request.getFullName());
        if (!StringUtils.hasText(fullName)) {
            throw new AppExceptions.BadRequestException("Passenger full name is required");
        }

        return OrderPassenger.builder()
                .fullName(fullName)
                .passengerType(normalizePassengerType(request.getPassengerType()))
                .identityNumber(normalizeText(request.getIdentityNumber()))
                .phoneNumber(normalizeText(request.getPhoneNumber()))
                .build();
    }

    private String normalizePassengerType(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? "ADULT" : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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

    private record AuthContext(UUID userId, int role, Set<String> scopes) {
        boolean isAdmin() {
            return role >= ROLE_ADMIN;
        }
    }
}
