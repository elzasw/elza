package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulApTypeDeclaration;
import cz.tacr.elza.domain.RulPackage;

/**
 * Repository of {@link RulApTypeDeclaration}.
 */
@Repository
public interface ApTypeDeclarationRepository extends JpaRepository<RulApTypeDeclaration, Integer> {

    List<RulApTypeDeclaration> findByRulPackage(RulPackage rulPackage);

    @Query("SELECT d FROM rul_ap_type_declaration d WHERE d.apType IN :apTypes")
    List<RulApTypeDeclaration> findByApTypes(@Param("apTypes") Collection<ApType> apTypes);
}
