package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemSpecAssignDeclaration;
import cz.tacr.elza.domain.RulItemSpecDeclaration;
import cz.tacr.elza.domain.RulItemType;

/**
 * Repository of {@link RulItemSpecAssignDeclaration}.
 */
@Repository
public interface ItemSpecAssignDeclarationRepository extends JpaRepository<RulItemSpecAssignDeclaration, Integer> {

    @Query("SELECT a FROM rul_item_spec_assign_declaration a WHERE a.specDeclaration IN :declarations")
    List<RulItemSpecAssignDeclaration> findByDeclarations(
            @Param("declarations") Collection<RulItemSpecDeclaration> declarations);

    @Query("SELECT a FROM rul_item_spec_assign_declaration a WHERE a.specDeclaration.itemSpec IN :itemSpecs")
    List<RulItemSpecAssignDeclaration> findByItemSpecs(@Param("itemSpecs") Collection<RulItemSpec> itemSpecs);

    @Query("SELECT a FROM rul_item_spec_assign_declaration a WHERE a.itemType IN :itemTypes")
    List<RulItemSpecAssignDeclaration> findByItemTypes(@Param("itemTypes") Collection<RulItemType> itemTypes);
}
