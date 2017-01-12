package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface ItemSpecRepository extends ElzaJpaRepository<RulItemSpec, Integer> {

    @Query("SELECT s FROM rul_item_spec s WHERE s.itemType in (?1)")
    List<RulItemSpec> findByItemTypeIds(Collection<RulItemType> itemTypes);

    List<RulItemSpec> findByItemType(RulItemType itemType);

    RulItemSpec getOneByCode(String code);


    List<RulItemSpec> findByRulPackage(RulPackage rulPackage);


    RulItemSpec findOneByCode(String code);

    RulItemSpec findByItemTypeAndCode(RulItemType itemType, String itemSpecCode);

    @Query("SELECT s FROM rul_item_spec s WHERE s.code IN :codes")
    List<RulItemSpec> findOneByCodes(@Param("codes") Collection<String> codes);
}
