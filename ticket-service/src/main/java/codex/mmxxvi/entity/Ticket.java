package codex.mmxxvi.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document("ticket")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EnableMongoAuditing
public class Ticket {
    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    private String title;
    @Field("date_start")
    private LocalDateTime dateStart ;
    @Field("date_end")
    private LocalDateTime dateEnd;
    @Builder.Default
    private Integer status = 0;
    private List<TicketItem> ticketItems;
    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt ;
    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt ;
    @Field("deleted_at")
    private LocalDateTime deletedAt ;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getDateStart() {
        return dateStart;
    }

    public void setDateStart(LocalDateTime dateStart) {
        this.dateStart = dateStart;
    }

    public LocalDateTime getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(LocalDateTime dateEnd) {
        this.dateEnd = dateEnd;
    }

    public List<TicketItem> getTicketItems() {
        return ticketItems;
    }

    public void setTicketItems(List<TicketItem> ticketItems) {
        this.ticketItems = ticketItems;
    }
}
