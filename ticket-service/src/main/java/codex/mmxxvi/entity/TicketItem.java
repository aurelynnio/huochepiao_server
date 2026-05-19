package codex.mmxxvi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Field("ticket_id")
    private UUID ticketId;

    private String name;

    private String description;

    @Field("coach_code")
    private String coachCode;

    @Field("seat_class")
    private String seatClass;

    @Field("seat_type")
    private String seatType;

    @Field("seat_labels")
    private List<String> seatLabels;

    @Field("available_seat_labels")
    private List<String> availableSeatLabels;

    @Field("stock_initial")
    private Integer stockInitial;

    @Field("stock_available")
    private Integer stockAvailable;

    @Field("is_stock_prepared")
    private boolean stockPrepared;

    @Field("price_original")
    private Long priceOriginal;

    @Field("price_flash")
    private Long priceFlash;

    @Field("sale_start_time")
    private LocalDateTime saleStartTime;

    @Field("sale_end_time")
    private LocalDateTime saleEndTime;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("deleted_at")
    private LocalDateTime deletedAt;

    public static TicketItemBuilder builder() {
        return new TicketItemBuilder();
    }

    public static class TicketItemBuilder {
        private UUID id;
        private UUID ticketId;
        private String name;
        private String description;
        private String coachCode;
        private String seatClass;
        private String seatType;
        private List<String> seatLabels;
        private List<String> availableSeatLabels;
        private Integer stockInitial;
        private Integer stockAvailable;
        private boolean stockPrepared;
        private Long priceOriginal;
        private Long priceFlash;
        private LocalDateTime saleStartTime;
        private LocalDateTime saleEndTime;

        public TicketItemBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public TicketItemBuilder ticketId(UUID ticketId) {
            this.ticketId = ticketId;
            return this;
        }

        public TicketItemBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TicketItemBuilder description(String description) {
            this.description = description;
            return this;
        }

        public TicketItemBuilder coachCode(String coachCode) {
            this.coachCode = coachCode;
            return this;
        }

        public TicketItemBuilder seatClass(String seatClass) {
            this.seatClass = seatClass;
            return this;
        }

        public TicketItemBuilder seatType(String seatType) {
            this.seatType = seatType;
            return this;
        }

        public TicketItemBuilder seatLabels(List<String> seatLabels) {
            this.seatLabels = seatLabels;
            return this;
        }

        public TicketItemBuilder availableSeatLabels(List<String> availableSeatLabels) {
            this.availableSeatLabels = availableSeatLabels;
            return this;
        }

        public TicketItemBuilder stockInitial(Integer stockInitial) {
            this.stockInitial = stockInitial;
            return this;
        }

        public TicketItemBuilder stockAvailable(Integer stockAvailable) {
            this.stockAvailable = stockAvailable;
            return this;
        }

        public TicketItemBuilder stockPrepared(boolean stockPrepared) {
            this.stockPrepared = stockPrepared;
            return this;
        }

        public TicketItemBuilder priceOriginal(Long priceOriginal) {
            this.priceOriginal = priceOriginal;
            return this;
        }

        public TicketItemBuilder priceFlash(Long priceFlash) {
            this.priceFlash = priceFlash;
            return this;
        }

        public TicketItemBuilder saleStartTime(LocalDateTime saleStartTime) {
            this.saleStartTime = saleStartTime;
            return this;
        }

        public TicketItemBuilder saleEndTime(LocalDateTime saleEndTime) {
            this.saleEndTime = saleEndTime;
            return this;
        }

        public TicketItem build() {
            TicketItem item = new TicketItem();
            item.id = this.id != null ? this.id : UUID.randomUUID();
            item.ticketId = this.ticketId;
            item.name = this.name;
            item.description = this.description;
            item.coachCode = this.coachCode;
            item.seatClass = this.seatClass;
            item.seatType = this.seatType;
            item.seatLabels = this.seatLabels;
            item.availableSeatLabels = this.availableSeatLabels;
            item.stockInitial = this.stockInitial;
            item.stockAvailable = this.stockAvailable;
            item.stockPrepared = this.stockPrepared;
            item.priceOriginal = this.priceOriginal;
            item.priceFlash = this.priceFlash;
            item.saleStartTime = this.saleStartTime;
            item.saleEndTime = this.saleEndTime;
            return item;
        }
    }
}
