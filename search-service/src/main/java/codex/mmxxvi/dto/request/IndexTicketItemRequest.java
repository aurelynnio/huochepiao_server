package codex.mmxxvi.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexTicketItemRequest {
    private UUID id;
    private UUID ticketId;
    private String name;
    private String description;
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
