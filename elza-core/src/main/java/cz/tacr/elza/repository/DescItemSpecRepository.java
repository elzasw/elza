package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulPackage;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DescItemSpecRepository extends JpaRepository<RulDescItemSpec, Integer> {

    @Query("SELECT s FROM rul_desc_item_spec s WHERE s.descItemType in (?1)")
    List<RulDescItemSpec> findByItemTypeIds(Collection<RulDescItemType> itemTypes);

    List<RulDescItemSpec> findByDescItemType(RulDescItemType rulDescItemType);

    RulDescItemSpec getOneByCode(String code);


    List<RulDescItemSpec> findByRulPackage(RulPackage rulPackage);


    RulDescItemSpec findOneByCode(String code);
}
