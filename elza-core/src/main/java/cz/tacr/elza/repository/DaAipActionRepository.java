package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaAipAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DaAipActionRepository extends JpaRepository<DaAipAction, Integer> {
}
