package profitas.profitas.backend.repository;

import profitas.profitas.backend.model.OverdraftLoan;
import profitas.profitas.backend.model.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OverdraftLoanRepository extends JpaRepository<OverdraftLoan, Long> {
    List<OverdraftLoan> findByCollateralAssetOwnerId(Long userId);
    List<OverdraftLoan> findByStatus(LoanStatus status);
}