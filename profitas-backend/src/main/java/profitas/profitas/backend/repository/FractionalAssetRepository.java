package profitas.profitas.backend.repository;

import profitas.profitas.backend.model.FractionalAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FractionalAssetRepository extends JpaRepository<FractionalAsset, Long> {
    List<FractionalAsset> findByOwnerId(Long userId);
}