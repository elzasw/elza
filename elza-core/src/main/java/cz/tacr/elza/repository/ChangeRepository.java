package cz.tacr.elza.repository;

import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrNode;


/**
 * Respozitory pro číslo změny.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface ChangeRepository extends ElzaJpaRepository<ArrChange, Integer> {

    void deleteByPrimaryNode(ArrNode node);
}
