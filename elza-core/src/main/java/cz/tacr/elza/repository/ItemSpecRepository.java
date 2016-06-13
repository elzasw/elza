package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface ItemSpecRepository extends JpaRepository<RulItemSpec, Integer> {

    @Query("SELECT s FROM rul_item_spec s WHERE s.itemType in (?1)")
    List<RulItemSpec> findByItemTypeIds(Collection<RulItemType> itemTypes);

    List<RulItemSpec> findByItemType(RulItemType itemType);

    RulItemSpec getOneByCode(String code);


    List<RulItemSpec> findByRulPackage(RulPackage rulPackage);


    RulItemSpec findOneByCode(String code);

    RulItemSpec findByItemTypeAndCode(RulItemType itemType, String itemSpecCode);
}
