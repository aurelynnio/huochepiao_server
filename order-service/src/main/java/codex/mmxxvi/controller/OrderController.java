package codex.mmxxvi.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import codex.mmxxvi.dto.request.CreateOrderRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.UpdateOrderRequest;
import codex.mmxxvi.dto.response.OrderResponse;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.services.OrderService;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public Mono<PageResponse<OrderResponse>> getAllOrders(
            @Valid @ModelAttribute PageRequestDto pageRequestDto,
            @RequestParam(required = false) Integer status
    ) {
        if (status != null) {
            return orderService.filterOrderFollowingStatus(status, pageRequestDto);
        }
        return orderService.getAllOrders(pageRequestDto);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @PatchMapping("/orders/{id}/status")
    public Mono<OrderResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateOrderRequest request) {
        return orderService.updateStatus(id, request.getStatus());
    }
}
