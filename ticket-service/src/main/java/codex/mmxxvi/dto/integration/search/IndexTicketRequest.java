package codex.mmxxvi.dto.integration.search;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexTicketRequest {
    private UUID id;
    private String title;
    private String trainNumber;
    private String departureStationCode;
    private String departureStationName;
    private String arrivalStationCode;
    private String arrivalStationName;
    private String journeyNote;
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private Integer status;
    private List<IndexTicketItemRequest> ticketItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
