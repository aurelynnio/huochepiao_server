package codex.mmxxvi.dto.integration.ticket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class TicketItemSnapshotResponse {
    private UUID ticketId;
    private UUID ticketItemId;
    private String ticketTitle;
    private String trainNumber;
    private String departureStationCode;
    private String departureStationName;
    private String arrivalStationCode;
    private String arrivalStationName;
    private Integer ticketStatus;
    private LocalDateTime ticketDateStart;
    private LocalDateTime ticketDateEnd;
    private String itemName;
    private String description;
    private String coachCode;
    private String seatClass;
    private String seatType;
    private List<String> seatLabels;
    private List<String> availableSeatLabels;
    private List<String> reservedSeatLabels;
    private Integer stockInitial;
    private Integer stockAvailable;
    private Boolean stockPrepared;
    private Long priceOriginal;
    private Long priceFlash;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
}
