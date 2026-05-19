package codex.mmxxvi.dto.request;

import java.time.LocalDate;

import lombok.Data;

@Data
public class SearchTicketRequest {
    private String departureStation;
    private String arrivalStation;
    private LocalDate departureDate;
    private String coachCode;
    private String seatClass;
    private String seatType;
    private Long minPrice;
    private Long maxPrice;
}
