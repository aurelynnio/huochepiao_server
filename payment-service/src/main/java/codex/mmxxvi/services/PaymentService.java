package codex.mmxxvi.services;

import codex.mmxxvi.dto.request.CreatePaymentRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.RefundRequest;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.PaymentInitResponse;
import codex.mmxxvi.dto.response.PaymentResponse;
import codex.mmxxvi.dto.response.RefundResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PaymentService {
    Mono<PaymentInitResponse> createPayment(CreatePaymentRequest createPaymentRequest, ServerHttpRequest request);
    Mono<PageResponse<PaymentResponse>> getPayments(PageRequestDto pageRequestDto);
    Mono<PaymentResponse> getPaymentByTransactionId(UUID transactionId);
    Mono<RefundResponse> refundPayment(UUID transactionId, RefundRequest refundRequest);
    Mono<Integer> handleCallback(ServerHttpRequest request);
}
