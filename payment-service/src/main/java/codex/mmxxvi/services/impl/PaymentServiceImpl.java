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
import codex.mmxxvi.services.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {
    private static final int STATUS_COMPLETED = 1;
    private static final int STATUS_FAILED = 2;
    private static final int STATUS_REFUNDED = 3;
    private static final DateTimeFormatter VNPAY_PAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String VNPAY_METHOD = "VNPAY";
    private static final int ROLE_ADMIN = 1;
    private static final int COMPACT_UUID_LENGTH = 32;

    private final PaymentRepository paymentRepository;
    private final VNPayConfig vnPayConfig;

    public PaymentServiceImpl(PaymentRepository paymentRepository, VNPayConfig vnPayConfig){
        this.paymentRepository = paymentRepository;
        this.vnPayConfig = vnPayConfig;
    }
    @Override
    public Mono<PaymentInitResponse> createPayment(CreatePaymentRequest createPaymentRequest, ServerHttpRequest request) {
        return resolveAuthContext().flatMap(authContext ->
            Mono.fromCallable(() -> {
                    requireAnyScope(authContext, "payment.write", "payment.write.self");

                    Payment payment = new Payment();
                    payment.setOrderId(createPaymentRequest.getOrderId());
                    payment.setUserId(authContext.userId());
                    payment.setAmount(createPaymentRequest.getAmount());
                    payment.setPaymentMethod(createPaymentRequest.getPaymentMethod());
                    payment.setStatus(createPaymentRequest.getStatus() != null ? createPaymentRequest.getStatus() : 0);
                    payment.setTransactionId(createPaymentRequest.getTransactionId() != null
                        ? createPaymentRequest.getTransactionId()
                        : UUID.randomUUID());
                    payment.setPaidAt(createPaymentRequest.getPaidAt());
                    Payment savedPayment = paymentRepository.save(payment);

                    return PaymentInitResponse.builder()
                        .payment(convertDTO(savedPayment))
                        .paymentUrl(buildPaymentUrl(savedPayment, request))
                        .build();
                })
                .subscribeOn(Schedulers.boundedElastic())
        );
    }

    private PaymentResponse convertDTO(Payment payment){
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private String buildPaymentUrl(Payment payment, ServerHttpRequest request) {
        if (!VNPAY_METHOD.equalsIgnoreCase(payment.getPaymentMethod())) {
            return null;
        }

        String returnUrl = buildReturnUrl(request);
        String clientIp = VNPayConfig.getIpAddress(request);
        String orderType = "other";

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(Math.multiplyExact(payment.getAmount(), 100L)));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", toVnPayTxnRef(payment.getTransactionId()));
        vnpParams.put("vnp_OrderInfo", payment.getOrderId().toString());
        vnpParams.put("vnp_OrderType", orderType);
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", clientIp);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String createdDate = VNPAY_PAY_DATE_FORMATTER.format(calendar.toInstant().atZone(calendar.getTimeZone().toZoneId()).toLocalDateTime());
        vnpParams.put("vnp_CreateDate", createdDate);

        calendar.add(Calendar.MINUTE, 15);
        String expireDate = VNPAY_PAY_DATE_FORMATTER.format(calendar.toInstant().atZone(calendar.getTimeZone().toZoneId()).toLocalDateTime());
        vnpParams.put("vnp_ExpireDate", expireDate);

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> iterator = fieldNames.iterator();
        while (iterator.hasNext()) {
            String fieldName = iterator.next();
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue == null || fieldValue.isBlank()) {
                continue;
            }

            hashData.append(fieldName)
                    .append('=')
                    .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

            if (iterator.hasNext()) {
                hashData.append('&');
                query.append('&');
            }
        }

        String secureHash = VNPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        return vnPayConfig.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    private String buildReturnUrl(ServerHttpRequest request) {
        String configuredReturnUrl = vnPayConfig.getReturnUrl();
        if (configuredReturnUrl.startsWith("http://") || configuredReturnUrl.startsWith("https://")) {
            return configuredReturnUrl;
        }

        var requestUri = request.getURI();
        StringBuilder baseUrl = new StringBuilder()
                .append(requestUri.getScheme())
                .append("://")
                .append(requestUri.getHost());

        int port = requestUri.getPort();
        if (port != -1 && port != 80 && port != 443) {
            baseUrl.append(":").append(port);
        }

        String contextPath = request.getPath().contextPath().value();
        String normalizedReturnUrl = configuredReturnUrl.startsWith("/") ? configuredReturnUrl : "/" + configuredReturnUrl;
        return baseUrl + contextPath + normalizedReturnUrl;
    }

    @Override
    public Mono<PageResponse<PaymentResponse>> getPayments(PageRequestDto pageRequestDto) {
        return resolveAuthContext().flatMap(authContext ->
            Mono.fromCallable(() -> {
                    requireAnyScope(authContext, "payment.read", "payment.read.self");

                    Pageable pageable = pageRequestDto.getPageable();
                    Page<Payment> paymentPage;
                    if (authContext.isAdmin() || authContext.scopes().contains("payment.read")) {
                    paymentPage = paymentRepository.findAll(pageable);
                    } else {
                    paymentPage = paymentRepository.findByUserId(authContext.userId(), pageable);
                    }

                    return PageResponse.<PaymentResponse>builder()
                        .data(paymentPage.getContent().stream()
                            .map(this::convertDTO).toList())
                        .pageNo(paymentPage.getNumber())
                        .pageSize(paymentPage.getSize())
                        .totalElements(paymentPage.getTotalElements())
                        .totalPages(paymentPage.getTotalPages())
                        .last(paymentPage.isLast())
                        .build();
                })
                .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<PaymentResponse> getPaymentByTransactionId(UUID transactionId) {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "payment.read", "payment.read.self");

                            Payment payment;
                            if (authContext.isAdmin() || authContext.scopes().contains("payment.read")) {
                                payment = paymentRepository.findByTransactionId(transactionId)
                                        .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Payment not found with transaction id: " + transactionId));
                            } else {
                                payment = paymentRepository.findByTransactionIdAndUserId(transactionId, authContext.userId())
                                        .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Payment not found with transaction id: " + transactionId));
                            }

                            return convertDTO(payment);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<RefundResponse> refundPayment(UUID transactionId, RefundRequest refundRequest) {
        return resolveAuthContext().flatMap(authContext ->
                Mono.fromCallable(() -> {
                            requireAnyScope(authContext, "payment.refund", "payment.refund.self");

                            UUID parsedTransactionId;
                            try {
                                parsedTransactionId = UUID.fromString(String.valueOf(transactionId));
                            } catch (IllegalArgumentException ex) {
                                throw new AppExceptions.BadRequestException("Invalid transaction id: " + transactionId);
                            }

                            Payment payment;
                            if (authContext.isAdmin() || authContext.scopes().contains("payment.refund")) {
                                payment = paymentRepository.findByTransactionId(parsedTransactionId)
                                        .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Payment not found with transaction id: " + transactionId));
                            } else {
                                payment = paymentRepository.findByTransactionIdAndUserId(parsedTransactionId, authContext.userId())
                                        .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Payment not found with transaction id: " + transactionId));
                            }

                            if (!payment.getId().equals(refundRequest.getPaymentId())) {
                                throw new AppExceptions.BadRequestException("Payment id does not match transaction id");
                            }

                            if (payment.getStatus() == STATUS_REFUNDED) {
                                throw new AppExceptions.ConflictException("Payment has already been refunded");
                            }

                            if (payment.getStatus() != STATUS_COMPLETED) {
                                throw new AppExceptions.BadRequestException("Only completed payments can be refunded");
                            }

                            if (refundRequest.getRefundAmount() == null || refundRequest.getRefundAmount() <= 0) {
                                throw new AppExceptions.BadRequestException("Refund amount must be greater than 0");
                            }

                            if (!refundRequest.getRefundAmount().equals(payment.getAmount())) {
                                throw new AppExceptions.BadRequestException("Partial refund is not supported");
                            }

                            payment.setStatus(STATUS_REFUNDED);
                            Payment refundedPayment = paymentRepository.save(payment);

                            return RefundResponse.builder()
                                    .paymentId(refundedPayment.getId())
                                    .orderId(refundedPayment.getOrderId())
                                    .refundAmount(refundRequest.getRefundAmount())
                                    .paymentMethod(refundedPayment.getPaymentMethod())
                                    .transactionId(refundedPayment.getTransactionId().toString())
                                    .status(refundedPayment.getStatus())
                                    .refundedAt(refundedPayment.getUpdatedAt())
                                    .build();
                        })
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    @Override
    public Mono<Integer> handleCallback(ServerHttpRequest request) {
        return Mono.fromCallable(() -> {
        Map<String, String> fields = new HashMap<>();
        request.getQueryParams().forEach((fieldName, values) -> {
            if ("vnp_SecureHash".equals(fieldName) || "vnp_SecureHashType".equals(fieldName)) {
                return;
            }
            String fieldValue = values == null || values.isEmpty() ? null : values.get(0);
            if (fieldValue != null && !fieldValue.isBlank()) {
                fields.put(fieldName, fieldValue);
            }
        });

        String secureHash = request.getQueryParams().getFirst("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            return -1;
        }

        String signValue = vnPayConfig.hashAllFields(fields);
        if (!signValue.equalsIgnoreCase(secureHash)) {
            return -1;
        }

        String transactionReference = request.getQueryParams().getFirst("vnp_TxnRef");
        if (transactionReference == null || transactionReference.isBlank()) {
            return -1;
        }

        UUID transactionId = parseVnPayTxnRef(transactionReference);
        if (transactionId == null) {
            return -1;
        }

        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Payment not found with transaction id: " + transactionId));

        String callbackStatus = request.getQueryParams().getFirst("vnp_TransactionStatus");
        if (callbackStatus == null || callbackStatus.isBlank()) {
            callbackStatus = request.getQueryParams().getFirst("vnp_ResponseCode");
        }

        boolean isSuccessful = "00".equals(callbackStatus);
        if (payment.getStatus() != STATUS_REFUNDED) {
            payment.setStatus(isSuccessful ? STATUS_COMPLETED : STATUS_FAILED);
        }

        if (isSuccessful) {
            LocalDateTime paidAt = parseVnPayDate(request.getQueryParams().getFirst("vnp_PayDate"));
            if (paidAt != null) {
                payment.setPaidAt(paidAt);
            }
        }

        paymentRepository.save(payment);
        return isSuccessful ? 1 : 0;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private LocalDateTime parseVnPayDate(String payDate) {
        if (payDate == null || payDate.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(payDate, VNPAY_PAY_DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String toVnPayTxnRef(UUID transactionId) {
        return transactionId.toString().replace("-", "");
    }

    private UUID parseVnPayTxnRef(String transactionReference) {
        if (transactionReference == null || transactionReference.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(transactionReference);
        } catch (IllegalArgumentException ignored) {
            // Continue with compact UUID parsing for VNPay-friendly references.
        }

        if (transactionReference.length() != COMPACT_UUID_LENGTH || !transactionReference.matches("[0-9a-fA-F]+")) {
            return null;
        }

        String normalized = transactionReference.substring(0, 8)
                + "-" + transactionReference.substring(8, 12)
                + "-" + transactionReference.substring(12, 16)
                + "-" + transactionReference.substring(16, 20)
                + "-" + transactionReference.substring(20);
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    private record AuthContext(UUID userId, int role, Set<String> scopes) {
        boolean isAdmin() {
            return role >= ROLE_ADMIN;
        }
    }
}
