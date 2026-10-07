package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.RulPartTypeDeclaration;

/**
 * Repository of {@link RulPartTypeDeclaration}.
 */
@Repository
public interface PartTypeDeclarationRepository extends JpaRepository<RulPartTypeDeclaration, Integer> {

    List<RulPartTypeDeclaration> findByRulPackage(RulPackage rulPackage);

    @Query("SELECT d FROM rul_part_type_declaration d WHERE d.partType IN :partTypes")
    List<RulPartTypeDeclaration> findByPartTypes(@Param("partTypes") Collection<RulPartType> partTypes);

    /** Declarations naming one of the part types as their child part. */
    @Query("SELECT d FROM rul_part_type_declaration d WHERE d.childPart IN :partTypes")
    List<RulPartTypeDeclaration> findByChildParts(@Param("partTypes") Collection<RulPartType> partTypes);
}
