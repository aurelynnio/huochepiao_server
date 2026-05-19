package codex.mmxxvi.dto.response;

import java.io.Serializable;

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
public class ResponseTicket implements Serializable {
    private static final long serialVersionUID = 1L;

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
    private List<TicketItem> ticketItems;

    public static ResponseTicketBuilder builder() {
        return new ResponseTicketBuilder();
    }

    public static class ResponseTicketBuilder {
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
        private List<TicketItem> ticketItems;

        public ResponseTicketBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public ResponseTicketBuilder title(String title) {
            this.title = title;
            return this;
        }

        public ResponseTicketBuilder trainNumber(String trainNumber) {
            this.trainNumber = trainNumber;
            return this;
        }

        public ResponseTicketBuilder departureStationCode(String departureStationCode) {
            this.departureStationCode = departureStationCode;
            return this;
        }

        public ResponseTicketBuilder departureStationName(String departureStationName) {
            this.departureStationName = departureStationName;
            return this;
        }

        public ResponseTicketBuilder arrivalStationCode(String arrivalStationCode) {
            this.arrivalStationCode = arrivalStationCode;
            return this;
        }

        public ResponseTicketBuilder arrivalStationName(String arrivalStationName) {
            this.arrivalStationName = arrivalStationName;
            return this;
        }

        public ResponseTicketBuilder journeyNote(String journeyNote) {
            this.journeyNote = journeyNote;
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

        public ResponseTicketBuilder status(Integer status) {
            this.status = status;
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
            response.trainNumber = this.trainNumber;
            response.departureStationCode = this.departureStationCode;
            response.departureStationName = this.departureStationName;
            response.arrivalStationCode = this.arrivalStationCode;
            response.arrivalStationName = this.arrivalStationName;
            response.journeyNote = this.journeyNote;
            response.dateStart = this.dateStart;
            response.dateEnd = this.dateEnd;
            response.status = this.status;
            response.ticketItems = this.ticketItems;
            return response;
        }
    }
}
