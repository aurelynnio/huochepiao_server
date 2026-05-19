package codex.mmxxvi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderPassengerRequest {

    @NotBlank(message = "Passenger full name is required")
    private String fullName;

    @Builder.Default
    private String passengerType = "ADULT";

    private String identityNumber;

    private String phoneNumber;
}
