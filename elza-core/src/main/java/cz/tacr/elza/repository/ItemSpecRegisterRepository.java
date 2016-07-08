package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

 /**
  * DescItemSpecRegister repository
  *
  * @author Petr Compel <petr.compel@marbes.cz>
  * @since 13.6.2016
  */
public interface ItemSpecRegisterRepository extends JpaRepository<RulItemSpecRegister, Integer> {

    /**
     * Vrátí registry k dané specifikaci
     *
     * @param itemSpec Specifikace
     * @return vrací Registry specifikace
     */
    @Query("SELECT ap FROM rul_item_spec_register ap WHERE ap.itemSpec = ?1")
    List<RulItemSpecRegister> findByDescItemSpecId(RulItemSpec itemSpec);

    void deleteByItemSpec(RulItemSpec itemSpec);
}
