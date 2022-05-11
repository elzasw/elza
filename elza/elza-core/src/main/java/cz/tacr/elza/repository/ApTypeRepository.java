package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.repository.vo.TypeRuleSet;


/**
 * Repository pro typ záznamu rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface ApTypeRepository extends JpaRepository<ApType, Integer>, ApTypeRepositoryCustom, Packaging<ApType> {

    /**
     * Najde všechny typy rejstříků seřazené podle názvu.
     *
     * @return všechyn typy rejstříků
     */
    @Query("SELECT r FROM ap_type r ORDER BY r.name ASC")
    List<ApType> findAllOrderByNameAsc();


    /**
     * Najde typ rejstříkového hesla podle kódu.
     *
     * @param apTypeCode kod
     * @return typ rejstříkového hesla
     */
    ApType findApTypeByCode(String apTypeCode);

    @Modifying
    @Query("UPDATE ap_type rr SET rr.parentApType = NULL WHERE rr.rulPackage = :rulPackage")
    void preDeleteByRulPackage(@Param("rulPackage") RulPackage rulPackage);

    // TODO: co to dělá?
    @Query("SELECT new cz.tacr.elza.repository.vo.TypeRuleSet(t.apTypeId, r.ruleSetId) FROM ap_type t JOIN t.rulPackage p JOIN rul_rule_set r ON r.rulPackage = p")
    List<TypeRuleSet> findTypeRuleSets();
}
