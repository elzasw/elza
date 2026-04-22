package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DaChangeRepository extends JpaRepository<DaChange, Integer> {
}
