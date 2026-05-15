package codex.mmxxvi.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageRequestDto {
    private int pageNo = 0;
    private int pageSize = 10;
    private String sortBy = "id";
    private String sortDir = "asc";

    public Pageable getPageable(){
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
        return PageRequest.of(pageNo,pageSize,sort);
    }
}
