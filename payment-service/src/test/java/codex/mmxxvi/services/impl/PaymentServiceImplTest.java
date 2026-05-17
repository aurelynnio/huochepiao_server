package codex.mmxxvi.services.impl;

import codex.mmxxvi.config.VNPayConfig;
import codex.mmxxvi.dto.request.CreatePaymentRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.RefundRequest;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.PaymentInitResponse;
import codex.mmxxvi.dto.response.PaymentResponse;
import codex.mmxxvi.dto.response.RefundResponse;
import codex.mmxxvi.entity.Payment;
import codex.mmxxvi.exception.AppExceptions;
import codex.mmxxvi.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
class PaymentServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PAYMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ORDER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TRANSACTION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private PaymentRepository paymentRepository;

    private VNPayConfig vnPayConfig;
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        vnPayConfig = new VNPayConfig();
        vnPayConfig.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        vnPayConfig.setReturnUrl("/v1/payments/vnpay/callback");
        vnPayConfig.setTmnCode("TESTMERCHANT");
        vnPayConfig.setHashSecret("secret");
        paymentService = new PaymentServiceImpl(paymentRepository, vnPayConfig);
    }

    @Test
    void createPaymentSavesAuthenticatedUserAndReturnsNullUrlForNonVnPayMethod() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(ORDER_ID)
                .amount(100_000L)
                .paymentMethod("COD")
                .transactionId(TRANSACTION_ID)
                .build();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(PAYMENT_ID);
            return payment;
        });

        PaymentInitResponse response = withAuth(
                paymentService.createPayment(request, request("http://localhost:8080/v1/payments")),
                USER_ID,
                0,
                "payment.write.self"
        );

        assertThat(response.getPaymentUrl()).isNull();
        assertThat(response.getPayment().getUserId()).isEqualTo(USER_ID);
        assertThat(response.getPayment().getTransactionId()).isEqualTo(TRANSACTION_ID);
    }

    @Test
    void createPaymentBuildsVnPayUrlForVnPayMethod() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(ORDER_ID)
                .amount(100_000L)
                .paymentMethod("VNPAY")
                .transactionId(TRANSACTION_ID)
                .build();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(PAYMENT_ID);
            return payment;
        });

        PaymentInitResponse response = withAuth(
                paymentService.createPayment(request, request("http://gateway.local:8080/v1/payments")),
                USER_ID,
                0,
                "payment.write.self"
        );

        assertThat(response.getPaymentUrl())
                .startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?")
                .contains("vnp_TmnCode=TESTMERCHANT")
                .contains("vnp_TxnRef=" + TRANSACTION_ID.toString().replace("-", ""))
                .contains("vnp_SecureHash=");
    }

    @Test
    void getPaymentsUsesUserFilterForSelfScope() {
        when(paymentRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment(0))));

        PageResponse<PaymentResponse> response = withAuth(
                paymentService.getPayments(new PageRequestDto()),
                USER_ID,
                0,
                "payment.read.self"
        );

        assertThat(response.getData()).hasSize(1);
        verify(paymentRepository).findByUserId(eq(USER_ID), any(Pageable.class));
    }

    @Test
    void getPaymentByTransactionIdUsesTransactionAndUserForSelfScope() {
        when(paymentRepository.findByTransactionIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(payment(1)));

        PaymentResponse response = withAuth(
                paymentService.getPaymentByTransactionId(TRANSACTION_ID),
                USER_ID,
                0,
                "payment.read.self"
        );

        assertThat(response.getTransactionId()).isEqualTo(TRANSACTION_ID);
    }

    @Test
    void refundPaymentMarksCompletedPaymentAsRefunded() {
        Payment payment = payment(1);
        when(paymentRepository.findByTransactionIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundResponse response = withAuth(
                paymentService.refundPayment(TRANSACTION_ID, RefundRequest.builder()
                        .paymentId(PAYMENT_ID)
                        .refundAmount(100_000L)
                        .build()),
                USER_ID,
                0,
                "payment.refund.self"
        );

        assertThat(response.getStatus()).isEqualTo(3);
        assertThat(payment.getStatus()).isEqualTo(3);
    }

    @Test
    void refundPaymentRejectsPartialRefund() {
        Payment payment = payment(1);
        when(paymentRepository.findByTransactionIdAndUserId(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> withAuth(
                paymentService.refundPayment(TRANSACTION_ID, RefundRequest.builder()
                        .paymentId(PAYMENT_ID)
                        .refundAmount(50_000L)
                        .build()),
                USER_ID,
                0,
                "payment.refund.self"
        )).isInstanceOf(AppExceptions.BadRequestException.class);
    }

    @Test
    void handleCallbackVerifiesSignatureAndMarksPaymentCompleted() {
        Payment payment = payment(0);
        when(paymentRepository.findByTransactionId(TRANSACTION_ID)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("vnp_TxnRef", TRANSACTION_ID.toString().replace("-", ""));
        fields.put("vnp_ResponseCode", "00");
        fields.put("vnp_PayDate", "20260515120000");
        String secureHash = vnPayConfig.hashAllFields(fields);

        Integer result = paymentService.handleCallback(MockServerHttpRequest
                .get("http://localhost:8080/v1/payments/vnpay/callback")
                .queryParam("vnp_TxnRef", fields.get("vnp_TxnRef"))
                .queryParam("vnp_ResponseCode", fields.get("vnp_ResponseCode"))
                .queryParam("vnp_PayDate", fields.get("vnp_PayDate"))
                .queryParam("vnp_SecureHash", secureHash)
                .build()).block();

        assertThat(result).isEqualTo(1);
        assertThat(payment.getStatus()).isEqualTo(1);
        assertThat(payment.getPaidAt()).isEqualTo(LocalDateTime.of(2026, 5, 15, 12, 0));
    }

    @Test
    void handleCallbackReturnsInvalidWhenSignatureIsMissing() {
        Integer result = paymentService.handleCallback(MockServerHttpRequest
                .get("http://localhost:8080/v1/payments/vnpay/callback")
                .queryParam("vnp_TxnRef", TRANSACTION_ID.toString())
                .build()).block();

        assertThat(result).isEqualTo(-1);
    }

    private Payment payment(int status) {
        return Payment.builder()
                .id(PAYMENT_ID)
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .amount(100_000L)
                .paymentMethod("VNPAY")
                .status(status)
                .transactionId(TRANSACTION_ID)
                .build();
    }

    private MockServerHttpRequest request(String uri) {
        return MockServerHttpRequest.post(uri)
                .header("x-forwarded-for", "203.0.113.10")
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
