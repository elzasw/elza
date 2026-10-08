package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecDeclaration;
import cz.tacr.elza.domain.RulPackage;

/**
 * Repository of {@link RulItemSpecDeclaration}.
 */
@Repository
public interface ItemSpecDeclarationRepository extends JpaRepository<RulItemSpecDeclaration, Integer> {

    List<RulItemSpecDeclaration> findByRulPackage(RulPackage rulPackage);

    @Query("SELECT d FROM rul_item_spec_declaration d WHERE d.itemSpec IN :itemSpecs")
    List<RulItemSpecDeclaration> findByItemSpecs(@Param("itemSpecs") Collection<RulItemSpec> itemSpecs);
}
