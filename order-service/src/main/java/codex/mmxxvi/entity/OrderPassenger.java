package codex.mmxxvi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPassenger {

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "passenger_type", nullable = false, length = 40)
    private String passengerType;

    @Column(name = "identity_number", length = 40)
    private String identityNumber;

    @Column(name = "phone_number", length = 24)
    private String phoneNumber;
}
