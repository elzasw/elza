package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemType;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DescItemTypeRepository extends JpaRepository<RulDescItemType, Integer> {

    @Query("SELECT DISTINCT t.dataType FROM rul_desc_item_type t WHERE t = ?1")
    List<RulDataType> findRulDataType(RulDescItemType descItemType);

}
