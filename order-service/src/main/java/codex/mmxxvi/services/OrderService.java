package codex.mmxxvi.services;

import java.util.UUID;

import codex.mmxxvi.dto.request.CreateOrderRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.response.OrderResponse;
import codex.mmxxvi.dto.response.PageResponse;
import reactor.core.publisher.Mono;


public interface OrderService {
    Mono<PageResponse<OrderResponse>> getAllOrders(PageRequestDto pageRequestDto);
    Mono<OrderResponse> createOrder(CreateOrderRequest request);
    Mono<OrderResponse> updateStatus(UUID id, Boolean status);
    Mono<PageResponse<OrderResponse>> filterOrderFollowingStatus(Integer status, PageRequestDto pageRequestDto);
}
