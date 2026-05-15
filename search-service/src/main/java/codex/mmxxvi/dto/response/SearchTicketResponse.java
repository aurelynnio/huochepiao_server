package codex.mmxxvi.dto.response;

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
public class SearchTicketResponse {
    private UUID id;
    private String title;
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private Integer status;
    private List<SearchTicketItemResponse> ticketItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Double score;
}
