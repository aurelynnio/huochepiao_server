package codex.mmxxvi.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {
    @NotBlank
    private String title;

    private String trainNumber;

    private String departureStationCode;

    private String departureStationName;

    private String arrivalStationCode;

    private String arrivalStationName;

    private String journeyNote;

    @NotNull
    private LocalDateTime dateStart;

    @NotNull
    private LocalDateTime dateEnd;

    @Valid
    @Builder.Default
    private List<UpdateTicketItemRequest> ticketItems = List.of();

    public String getTitle() {
        return title;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public String getDepartureStationCode() {
        return departureStationCode;
    }

    public String getDepartureStationName() {
        return departureStationName;
    }

    public String getArrivalStationCode() {
        return arrivalStationCode;
    }

    public String getArrivalStationName() {
        return arrivalStationName;
    }

    public String getJourneyNote() {
        return journeyNote;
    }

    public LocalDateTime getDateStart() {
        return dateStart;
    }

    public LocalDateTime getDateEnd() {
        return dateEnd;
    }

    public List<UpdateTicketItemRequest> getTicketItems() {
        return ticketItems;
    }
}
