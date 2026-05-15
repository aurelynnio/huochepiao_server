package codex.mmxxvi.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDto {
    @Min(value = 0, message = "pageNo must be greater than or equal to 0")
    private int pageNo = 0;

    @Min(value = 1, message = "pageSize must be greater than 0")
    @Max(value = 100, message = "pageSize must be less than or equal to 100")
    private int pageSize = 10;

    private String sortBy = "id";

    @Pattern(regexp = "(?i)asc|desc", message = "sortDir must be asc or desc")
    private String sortDir = "asc";

    public Pageable getPageable(){
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(pageNo,pageSize,sort);
    }
}
