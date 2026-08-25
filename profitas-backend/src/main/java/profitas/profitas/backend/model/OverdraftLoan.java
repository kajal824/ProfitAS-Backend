package profitas.profitas.backend.model;

import profitas.profitas.backend.model.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "overdraft_loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverdraftLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private FractionalAsset collateralAsset;

    @Column(nullable = false)
    private BigDecimal loanAmount; // Up to 70% LTV

    @Column(nullable = false)
    private BigDecimal interestRate; // e.g., 11.00 (% p.a.)

    @Column(nullable = false)
    private BigDecimal remainingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = LoanStatus.ACTIVE;
    }
}