package profitas.profitas.backend.repository;

import jakarta.persistence.LockModeType;
import profitas.profitas.backend.model.FractionalAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FractionalAssetRepository extends JpaRepository<FractionalAsset, Long> {
    List<FractionalAsset> findByOwnerId(Long userId);

    /** Row lock so two concurrent loans/exits on one asset can't both pass the checks. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from FractionalAsset a where a.id = :id")
    Optional<FractionalAsset> findByIdForUpdate(@Param("id") Long id);
}
