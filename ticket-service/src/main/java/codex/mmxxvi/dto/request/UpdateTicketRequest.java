package codex.mmxxvi.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequest {
    private String title;
    private String trainNumber;
    private String departureStationCode;
    private String departureStationName;
    private String arrivalStationCode;
    private String arrivalStationName;
    private String journeyNote;
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;

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
}
