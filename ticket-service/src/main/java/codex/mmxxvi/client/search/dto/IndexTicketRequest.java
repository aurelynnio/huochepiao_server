package codex.mmxxvi.client.search.dto;

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
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private Integer status;
    private List<IndexTicketItemRequest> ticketItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
