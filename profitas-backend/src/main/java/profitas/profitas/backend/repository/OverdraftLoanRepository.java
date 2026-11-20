package profitas.profitas.backend.repository;

import profitas.profitas.backend.model.OverdraftLoan;
import profitas.profitas.backend.model.enums.LoanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OverdraftLoanRepository extends JpaRepository<OverdraftLoan, Long> {
    List<OverdraftLoan> findByCollateralAssetOwnerId(Long userId);
    List<OverdraftLoan> findByStatus(LoanStatus status);
    List<OverdraftLoan> findByCollateralAssetIdAndStatus(Long assetId, LoanStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from OverdraftLoan l where l.id = :id")
    Optional<OverdraftLoan> findByIdForUpdate(@Param("id") Long id);
}
