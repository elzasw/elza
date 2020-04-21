package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPartType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repozitory pro {@link RulPartType}
 *
 * @since 20.04.2020
 */
@Repository
public interface PartTypeRepository extends JpaRepository<RulPartType, Integer>, Packaging<RulPartType> {

    RulPartType findByCode(String code);
}
