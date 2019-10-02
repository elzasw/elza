package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ApType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.RulPackage;


/**
 * Repository pro typ záznamu rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface ApTypeRepository extends JpaRepository<ApType, Integer>, ApTypeRepositoryCustom, Packaging<ApType> {

    /**
     * Najde všechny typy rejstříkových hesel, které jsou napojeny na typy osob.
     *
     * @return seznam typů rejstříkových hesel
     */
    @Query("SELECT r FROM ap_type r WHERE r.partyType IS NOT null ORDER BY r.name ASC")
    List<ApType> findTypesForPartyTypes();


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

    /**
     * Typ rejstříku, který odpovídá typu osoby.
     * @param partyType typ osoby ke kterému hledáme příslušný typ rejstříku
     * @return      nalezené rejstříkové typy k danému typu osoby
     */
    List<ApType> findApTypeByPartyType(ParPartyType partyType);


    /**
     * Hledá typy, které mají true pro přidání záznamu a jsou daného typu osoby.
     *
     * @param partyType typ osoby
     * @return seznam typů
     */
    List<ApType> findByPartyTypeAndReadOnlyFalseOrderByName(ParPartyType partyType);


    /**
     * Hledá typy, které mají true pro přidání záznamu a nejsou pro typ osob.
     *
     * @return seznam typů
     */
    List<ApType> findByPartyTypeIsNullAndReadOnlyFalseOrderByName();


    /**
     * Najde typy rejstříků napojené na typ vztahu.
     *
     * @param relationRoleType typ vztahu
     * @return typy rejstříků
     */
    @Query("SELECT rr.apType FROM par_registry_role rr WHERE rr.roleType = ?1")
    List<ApType> findByRelationRoleType(ParRelationRoleType relationRoleType);

    @Modifying
    @Query("UPDATE ap_type rr SET rr.parentApType = NULL WHERE rr.rulPackage = :rulPackage")
    void preDeleteByRulPackage(@Param("rulPackage") RulPackage rulPackage);

    @Query("SELECT count(t) FROM ap_type t WHERE t.partyType IS NOT NULL AND t.apTypeId IN (:ids)")
    Integer findCountPartyTypeNotNullByIds(@Param("ids") Set<Integer> ids);
}
