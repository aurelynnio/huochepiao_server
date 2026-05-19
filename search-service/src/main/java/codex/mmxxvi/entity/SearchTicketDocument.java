package codex.mmxxvi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ticket-search")
public class SearchTicketDocument {
    @Id
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
    private List<SearchTicketItemDocument> ticketItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
