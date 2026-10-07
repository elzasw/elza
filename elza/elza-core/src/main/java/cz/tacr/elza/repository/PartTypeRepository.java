package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulPartType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repozitory pro {@link RulPartType}. Part types are declared by packages
 * ({@link PartTypeDeclarationRepository}); the package of the row is only the winning declaration.
 *
 * @since 20.04.2020
 */
@Repository
public interface PartTypeRepository extends JpaRepository<RulPartType, Integer> {

    RulPartType findByCode(String code);
}
