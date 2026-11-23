package profitas.profitas.backend.repository;

import profitas.profitas.backend.model.InstantExitRequest;
import profitas.profitas.backend.model.enums.ExitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface InstantExitRequestRepository extends JpaRepository<InstantExitRequest, Long> {
    List<InstantExitRequest> findByAssetOwnerId(Long userId);
    List<InstantExitRequest> findByStatus(ExitStatus status);
    boolean existsByAssetIdAndStatusIn(Long assetId, Collection<ExitStatus> statuses);
}
