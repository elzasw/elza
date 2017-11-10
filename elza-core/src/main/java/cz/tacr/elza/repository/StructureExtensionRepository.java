package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureExtension;
import cz.tacr.elza.domain.RulStructureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link RulStructureExtension}
 *
 * @since 30.10.2017
 */
@Repository
public interface StructureExtensionRepository extends JpaRepository<RulStructureExtension, Integer>, Packaging<RulStructureExtension> {

    List<RulStructureExtension> findByRulPackageAndStructureTypeIn(RulPackage rulPackage, List<RulStructureType> rulStructureTypes);
}
