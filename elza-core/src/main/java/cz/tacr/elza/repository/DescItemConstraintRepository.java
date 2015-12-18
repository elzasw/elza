package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulDescItemConstraint;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.RulPackage;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Repository
public interface DescItemConstraintRepository extends JpaRepository<RulDescItemConstraint, Integer> {

    @Query("SELECT s FROM rul_desc_item_constraint s WHERE s.descItemType in (?1)")
    List<RulDescItemConstraint> findByItemTypeIds(Collection<RulDescItemType> itemTypes);


    @Query("SELECT s FROM rul_desc_item_constraint s WHERE s.descItemSpec in (?1)")
    List<RulDescItemConstraint> findByItemSpecIds(Collection<RulDescItemSpec> itemSpecs);


    List<RulDescItemConstraint> findByDescItemSpec(RulDescItemSpec rulDescItemSpec);


    List<RulDescItemConstraint> findByDescItemType(RulDescItemType rulDescItemType);


    List<RulDescItemConstraint> findByRulPackage(RulPackage rulPackage);


    void deleteByRulPackage(RulPackage rulPackage);
}
