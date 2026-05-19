package codex.mmxxvi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchTicketItemDocument {
    private UUID id;
    private UUID ticketId;
    private String name;
    private String description;
    private String coachCode;
    private String seatClass;
    private String seatType;
    private List<String> seatLabels;
    private List<String> availableSeatLabels;
    private Integer stockInitial;
    private Integer stockAvailable;
    private Boolean stockPrepared;
    private Long priceOriginal;
    private Long priceFlash;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
