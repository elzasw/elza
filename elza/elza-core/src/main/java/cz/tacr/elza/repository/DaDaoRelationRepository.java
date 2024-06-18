package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DaDaoRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DaDaoRelationRepository extends JpaRepository<DaDaoRelation, Integer> {
}
