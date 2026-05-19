package codex.mmxxvi.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private UUID userId;
    private UUID ticketId;
    private UUID ticketItemId;
    private String ticketTitle;
    private String trainNumber;
    private String departureStationCode;
    private String departureStationName;
    private String arrivalStationCode;
    private String arrivalStationName;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String coachCode;
    private String seatClass;
    private String seatType;
    private Integer quantity;
    private Long unitPrice;
    private Long totalPrice;
    private String ticketCode;
    private String qrPayload;
    private List<String> seatLabels;
    private List<OrderPassengerResponse> passengers;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
