package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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


    @Modifying
    @Query("DELETE FROM arr_change c WHERE c.primaryNode IN (SELECT n FROM arr_node n WHERE n.fund = ?1)")
    void deleteByPrimaryNodeFund(ArrFund fund);
}
