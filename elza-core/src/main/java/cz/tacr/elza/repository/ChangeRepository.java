package cz.tacr.elza.repository;

import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;


/**
 * Respozitory pro číslo změny.
 * 
 * @since 22.7.15
 */
@Repository
public interface ChangeRepository extends ElzaJpaRepository<ArrChange, Integer> {


    void deleteByPrimaryNodeFund(ArrFund fund);
}
