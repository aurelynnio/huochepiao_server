package codex.mmxxvi.services.impl;

import codex.mmxxvi.dto.request.CreateOrderRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.response.OrderResponse;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.entity.Order;
import codex.mmxxvi.exception.AppExceptions;
import codex.mmxxvi.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ORDER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TICKET_ITEM_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private OrderRepository orderRepository;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository);
    }

    @Test
    void getAllOrdersUsesFindAllForAdminReadScope() {
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(order(USER_ID, 0))));

        PageResponse<OrderResponse> response = withAuth(
                orderService.getAllOrders(new PageRequestDto()),
                OTHER_USER_ID,
                1,
                "order.read"
        );

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getUserId()).isEqualTo(USER_ID);
        verify(orderRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAllOrdersUsesUserFilterForSelfScope() {
        when(orderRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order(USER_ID, 0))));

        PageResponse<OrderResponse> response = withAuth(
                orderService.getAllOrders(new PageRequestDto()),
                USER_ID,
                0,
                "order.read.self"
        );

        assertThat(response.getContent()).hasSize(1);
        verify(orderRepository).findByUserId(eq(USER_ID), any(Pageable.class));
    }

    @Test
    void createOrderRejectsCreatingForAnotherUserWithoutAdminRole() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(OTHER_USER_ID)
                .ticketItemId(TICKET_ITEM_ID)
                .quantity(2)
                .unitPrice(50_000L)
                .totalPrice(100_000L)
                .build();

        assertThatThrownBy(() -> withAuth(orderService.createOrder(request), USER_ID, 0, "order.write.self"))
                .isInstanceOf(AppExceptions.ForbiddenException.class);
    }

    @Test
    void createOrderSavesPendingOrderWhenStatusIsMissing() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .userId(USER_ID)
                .ticketItemId(TICKET_ITEM_ID)
                .quantity(2)
                .unitPrice(50_000L)
                .totalPrice(100_000L)
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return order;
        });

        OrderResponse response = withAuth(orderService.createOrder(request), USER_ID, 0, "order.write.self");

        assertThat(response.getId()).isEqualTo(ORDER_ID);
        assertThat(response.getStatus()).isZero();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateStatusRequiresAdminScopeAndRole() {
        Order order = order(USER_ID, 0);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = withAuth(orderService.updateStatus(ORDER_ID, true), OTHER_USER_ID, 1, "order.admin");

        assertThat(response.getStatus()).isEqualTo(1);
    }

    @Test
    void filterOrdersByStatusUsesUserAndStatusForSelfScope() {
        when(orderRepository.findByUserIdAndStatus(eq(USER_ID), eq(0), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order(USER_ID, 0))));

        PageResponse<OrderResponse> response = withAuth(
                orderService.filterOrderFollowingStatus(0, new PageRequestDto()),
                USER_ID,
                0,
                "order.read.self"
        );

        assertThat(response.getContent()).hasSize(1);
        verify(orderRepository).findByUserIdAndStatus(eq(USER_ID), eq(0), any(Pageable.class));
    }

    private Order order(UUID userId, int status) {
        return Order.builder()
                .id(ORDER_ID)
                .userId(userId)
                .ticketItemId(TICKET_ITEM_ID)
                .quantity(2)
                .unitPrice(50_000L)
                .totalPrice(100_000L)
                .status(status)
                .build();
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
}
