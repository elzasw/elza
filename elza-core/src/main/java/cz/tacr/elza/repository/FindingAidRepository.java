package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFindingAid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface FindingAidRepository extends JpaRepository<ArrFindingAid, Integer> {

}
