package profitas.profitas.backend.model;

import profitas.profitas.backend.model.enums.ExitStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "instant_exit_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstantExitRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private FractionalAsset asset;

    @Column(nullable = false)
    private BigDecimal originalValuation;

    @Column(nullable = false)
    private BigDecimal haircutPercentage; // Default: 3.00

    @Column(nullable = false)
    private BigDecimal payoutAmount; // Calculated as originalValuation * (1 - haircut)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExitStatus status;

    private LocalDateTime requestedAt;
    private LocalDateTime settledAt;

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
        this.status = ExitStatus.PENDING;
    }
}