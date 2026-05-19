package codex.mmxxvi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPassengerResponse {
    private String fullName;
    private String passengerType;
    private String identityNumber;
    private String phoneNumber;
}
