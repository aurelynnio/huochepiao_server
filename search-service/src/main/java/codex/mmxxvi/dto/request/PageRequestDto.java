package codex.mmxxvi.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDto {
    @Min(value = 0, message = "pageNo must be greater than or equal to 0")
    private int pageNo = 0;

    @Min(value = 1, message = "pageSize must be greater than 0")
    @Max(value = 100, message = "pageSize must be less than or equal to 100")
    private int pageSize = 10;
}
