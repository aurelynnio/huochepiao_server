package codex.mmxxvi.services;

import java.util.UUID;

import codex.mmxxvi.dto.request.CreateOrderRequest;
import codex.mmxxvi.dto.internal.InternalOrderResponse;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.response.OrderResponse;
import codex.mmxxvi.dto.response.PageResponse;
import reactor.core.publisher.Mono;


public interface OrderService {
    Mono<PageResponse<OrderResponse>> getAllOrders(PageRequestDto pageRequestDto);
    Mono<OrderResponse> createOrder(CreateOrderRequest request);
    Mono<OrderResponse> updateStatus(UUID id, Integer status);
    Mono<OrderResponse> cancelOrder(UUID id);
    Mono<PageResponse<OrderResponse>> filterOrderFollowingStatus(Integer status, PageRequestDto pageRequestDto);
    Mono<InternalOrderResponse> getOrderInternal(UUID id);
    Mono<OrderResponse> updateStatusInternal(UUID id, Integer status);
}
