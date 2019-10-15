package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructuredType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repozitory pro {@link RulStructuredType}
 *
 * @since 27.10.2017
 */
@Repository
public interface StructuredTypeRepository extends JpaRepository<RulStructuredType, Integer>, Packaging<RulStructuredType> {

    RulStructuredType findByCode(String code);
}
