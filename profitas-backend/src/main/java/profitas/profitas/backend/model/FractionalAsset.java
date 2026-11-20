package profitas.profitas.backend.model;

import profitas.profitas.backend.model.enums.AssetStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fractional_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FractionalAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String assetName; // e.g., "Grade-A Tech Park Unit 4B"

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentValuation;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyRentalIncome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AssetStatus status = AssetStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;
}
