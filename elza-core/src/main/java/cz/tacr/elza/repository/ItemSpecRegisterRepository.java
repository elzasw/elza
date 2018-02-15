package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecRegister;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

/**
  * ItemSpecRegister repository
  *
  * @author Petr Compel <petr.compel@marbes.cz>
  * @since 13.6.2016
  */
public interface ItemSpecRegisterRepository extends ElzaJpaRepository<RulItemSpecRegister, Integer> {

    /**
     * Vrátí registry k dané specifikaci
     *
     * @param itemSpec Specifikace
     * @return vrací Registry specifikace
     */
    @Query("SELECT ap FROM rul_item_spec_register ap WHERE ap.itemSpec = ?1")
    List<RulItemSpecRegister> findByDescItemSpecId(RulItemSpec itemSpec);

    /**
     * Vrátí registry k dané specifikaci
     *
     * @param itemSpec Specifikace
     * @return vrací Registry specifikace
     */
    @Query("SELECT ap.apType.apTypeId FROM rul_item_spec_register ap WHERE ap.itemSpec = ?1")
    Set<Integer> findIdsByItemSpecId(RulItemSpec itemSpec);

    @Query("SELECT ap.apType FROM rul_item_spec_register ap WHERE ap.itemSpec = ?1")
    Set<ApType> findByItemSpecId(RulItemSpec itemSpec);

    void deleteByItemSpec(RulItemSpec itemSpec);
}
