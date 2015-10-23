package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemSpecRegister;

/**
 * Repository pro typy osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface DescItemSpecRegisterRepository extends JpaRepository<RulDescItemSpecRegister, Integer> {

    /**
     * @param recordId  id záznamu rejtříku
     * @return  záznamy patřící danému záznamu v rejstříku
     */
    @Query("SELECT ap FROM rul_desc_item_spec_register ap WHERE ap.descItemSpec = ?1")
    List<RulDescItemSpecRegister> findByDescItemSpecId(RulDescItemSpec descItemSpec);
}
