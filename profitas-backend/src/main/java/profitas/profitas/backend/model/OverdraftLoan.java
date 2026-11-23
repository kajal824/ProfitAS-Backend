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

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal loanAmount; // Up to 70% LTV

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate; // e.g., 11.00 (% p.a.)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance; // principal + accrued interest - repayments

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    private LocalDateTime createdAt;

    /** Interest has been applied to remainingBalance up to this moment (whole days). */
    private LocalDateTime lastAccrualAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (lastAccrualAt == null) lastAccrualAt = now;
        if (status == null) status = LoanStatus.ACTIVE;
    }
}
