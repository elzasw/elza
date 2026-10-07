package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeDeclaration;
import cz.tacr.elza.domain.RulPackage;

/**
 * Repository of {@link RulItemTypeDeclaration}.
 */
@Repository
public interface ItemTypeDeclarationRepository extends JpaRepository<RulItemTypeDeclaration, Integer> {

    List<RulItemTypeDeclaration> findByRulPackage(RulPackage rulPackage);

    @Query("SELECT d FROM rul_item_type_declaration d WHERE d.itemType IN :itemTypes")
    List<RulItemTypeDeclaration> findByItemTypes(@Param("itemTypes") Collection<RulItemType> itemTypes);
}
