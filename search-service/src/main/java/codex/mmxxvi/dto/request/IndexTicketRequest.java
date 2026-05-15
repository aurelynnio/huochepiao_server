package codex.mmxxvi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndexTicketRequest {
    @NotNull(message = "id is required")
    private UUID id;
    private String title;
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private Integer status;
    @Valid
    private List<IndexTicketItemRequest> ticketItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
