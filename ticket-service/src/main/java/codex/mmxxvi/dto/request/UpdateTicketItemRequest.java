package codex.mmxxvi.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketItemRequest {
    private UUID id;
    private String name;
    private String description;

    @PositiveOrZero
    private Integer stockInitial;

    @PositiveOrZero
    private Integer stockAvailable;

    private Boolean stockPrepared;

    @PositiveOrZero
    private Long priceOriginal;

    @PositiveOrZero
    private Long priceFlash;

    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;


    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getStockInitial() {
        return stockInitial;
    }

    public Integer getStockAvailable() {
        return stockAvailable;
    }

    public Boolean getStockPrepared() {
        return stockPrepared;
    }

    public Long getPriceOriginal() {
        return priceOriginal;
    }

    public Long getPriceFlash() {
        return priceFlash;
    }

    public LocalDateTime getSaleStartTime() {
        return saleStartTime;
    }

    public LocalDateTime getSaleEndTime() {
        return saleEndTime;
    }
}
