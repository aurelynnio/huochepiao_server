package codex.mmxxvi.dto.response;

import codex.mmxxvi.entity.TicketItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseTicket {
    private UUID id;
    private String title;
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private List<TicketItem> ticketItems;

    public static ResponseTicketBuilder builder() {
        return new ResponseTicketBuilder();
    }

    public static class ResponseTicketBuilder {
        private UUID id;
        private String title;
        private LocalDateTime dateStart;
        private LocalDateTime dateEnd;
        private List<TicketItem> ticketItems;

        public ResponseTicketBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public ResponseTicketBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ResponseTicketBuilder dateStart(LocalDateTime dateStart) {
            this.dateStart = dateStart;
            return this;
        }

        public ResponseTicketBuilder dateEnd(LocalDateTime dateEnd) {
            this.dateEnd = dateEnd;
            return this;
        }

        public ResponseTicketBuilder ticketItems(List<TicketItem> ticketItems) {
            this.ticketItems = ticketItems;
            return this;
        }

        public ResponseTicket build() {
            ResponseTicket response = new ResponseTicket();
            response.id = this.id;
            response.title = this.title;
            response.dateStart = this.dateStart;
            response.dateEnd = this.dateEnd;
            response.ticketItems = this.ticketItems;
            return response;
        }
    }
}
