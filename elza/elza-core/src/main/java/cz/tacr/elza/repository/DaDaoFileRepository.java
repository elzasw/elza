package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaDaoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DaDaoFileRepository extends JpaRepository<DaDaoFile, Integer> {
}
