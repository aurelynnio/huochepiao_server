package codex.mmxxvi.services.impl;

import codex.mmxxvi.dto.request.CreateOrderRequest;
import codex.mmxxvi.repository.OrderRepository;
import codex.mmxxvi.exception.AppExceptions;
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
import codex.mmxxvi.dto.response.OrderResponse;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.entity.Order;
import codex.mmxxvi.services.OrderService;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private static final int ROLE_ADMIN = 1;


    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    private OrderResponse convertDTO(Order order){
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .ticketItemId(order.getTicketItemId())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalPrice(order.getTotalPrice())
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

                            Order order = Order.builder()
                                    .userId(request.getUserId())
                                    .ticketItemId(request.getTicketItemId())
                                    .quantity(request.getQuantity())
                                    .unitPrice(request.getUnitPrice())
                                    .totalPrice(request.getTotalPrice())
                                    .status(request.getStatus() == null ? 0 : request.getStatus())
                                    .build();
                            return convertDTO(orderRepository.save(order));
                        })
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<OrderResponse> updateStatus(UUID id, Boolean status) {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "order.admin");
                            requireAdmin(authContext);

                            Order order = orderRepository.findById(id)
                                    .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Order not found"));
                            order.setStatus(Boolean.TRUE.equals(status) ? 1 : 0);
                            return convertDTO(orderRepository.save(order));
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
