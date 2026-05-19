package codex.mmxxvi.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ticket_item_id", nullable = false, length = 36)
    private UUID ticketItemId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ticket_id", nullable = false, length = 36)
    private UUID ticketId;

    @Column(name = "ticket_title", nullable = false, length = 180)
    private String ticketTitle;

    @Column(name = "train_number", length = 64)
    private String trainNumber;

    @Column(name = "departure_station_code", length = 24)
    private String departureStationCode;

    @Column(name = "departure_station_name", length = 120)
    private String departureStationName;

    @Column(name = "arrival_station_code", length = 24)
    private String arrivalStationCode;

    @Column(name = "arrival_station_name", length = 120)
    private String arrivalStationName;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "coach_code", length = 48)
    private String coachCode;

    @Column(name = "seat_class", length = 80)
    private String seatClass;

    @Column(name = "seat_type", length = 80)
    private String seatType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "ticket_code", length = 24, unique = true)
    private String ticketCode;

    @Column(name = "qr_payload", length = 2000)
    private String qrPayload;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_seat_labels", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "seat_label", length = 32, nullable = false)
    @Builder.Default
    private List<String> seatLabels = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_passengers", joinColumns = @JoinColumn(name = "order_id"))
    @Builder.Default
    private List<OrderPassenger> passengers = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private Integer status = 0; // 0: pending, 1: completed, 2: failed, 3: refunded, 4: cancelled

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
