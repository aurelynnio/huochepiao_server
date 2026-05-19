package codex.mmxxvi.integration.order;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import codex.mmxxvi.dto.integration.order.InternalOrderResponse;
import codex.mmxxvi.dto.integration.order.UpdateOrderStatusRequest;

@FeignClient(
        name = "order-service",
        url = "${ORDER_SERVICE_URL:http://order-service:8083}",
        path = "/v1/internal/orders"
)
public interface OrderClient {

    @GetMapping("/{orderId}")
    InternalOrderResponse getOrder(
            @RequestHeader("X-Internal-Key") String internalApiKey,
            @PathVariable UUID orderId
    );

    @PatchMapping("/{orderId}/status")
    void updateOrderStatus(
            @RequestHeader("X-Internal-Key") String internalApiKey,
            @PathVariable UUID orderId,
            @RequestBody UpdateOrderStatusRequest request
    );
}
