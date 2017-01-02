package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulPackage;


/**
 * Repository pro typ záznamu rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface RegisterTypeRepository extends JpaRepository<RegRegisterType, Integer>, RegisterTypeRepositoryCustom, Packaging<RegRegisterType> {

    /**
     * Najde všechny typy rejstříkových hesel, které jsou napojeny na typy osob.
     *
     * @return seznam typů rejstříkových hesel
     */
    @Query("SELECT r FROM reg_register_type r WHERE r.partyType IS NOT null ORDER BY r.name ASC")
    List<RegRegisterType> findTypesForPartyTypes();


    /**
     * Najde všechny typy rejstříků seřazené podle názvu.
     *
     * @return všechyn typy rejstříků
     */
    @Query("SELECT r FROM reg_register_type r ORDER BY r.name ASC")
    List<RegRegisterType> findAllOrderByNameAsc();


    /**
     * Najde typ rejstříkového hesla podle kódu.
     *
     * @param registerTypeCode kod
     * @return typ rejstříkového hesla
     */
    RegRegisterType findRegisterTypeByCode(String registerTypeCode);

    /**
     * Typ rejstříku, který odpovídá typu osoby.
     * @param partyType typ osoby ke kterému hledáme příslušný typ rejstříku
     * @return      nalezené rejstříkové typy k danému typu osoby
     */
    List<RegRegisterType> findRegisterTypeByPartyType(ParPartyType partyType);


    /**
     * Hledá typy, které mají true pro přidání záznamu a jsou daného typu osoby.
     *
     * @param partyType typ osoby
     * @return seznam typů
     */
    @Query("SELECT t FROM reg_register_type t WHERE t.partyType = ?1 AND t.addRecord = true ORDER BY t.name ASC")
    List<RegRegisterType> findByPartyTypeEnableAdding(ParPartyType partyType);


    /**
     * Hledá typy, které mají true pro přidání záznamu a nejsou pro typ osob.
     *
     * @return seznam typů
     */
    @Query("SELECT t FROM reg_register_type t WHERE t.partyType IS NULL AND t.addRecord = true ORDER BY t.name ASC")
    List<RegRegisterType> findNullPartyTypeEnableAdding();


    /**
     * Najde typy rejstříků napojené na typ vztahu.
     *
     * @param relationRoleType typ vztahu
     * @return typy rejstříků
     */
    @Query("SELECT rr.registerType FROM par_registry_role rr WHERE rr.roleType = ?1")
    List<RegRegisterType> findByRelationRoleType(ParRelationRoleType relationRoleType);

    @Modifying
    @Query("UPDATE reg_register_type rr SET rr.parentRegisterType = NULL WHERE rr.rulPackage = :rulPackage")
    void preDeleteByRulPackage(@Param("rulPackage") RulPackage rulPackage);

    /**
     * Najde typ rejstříkového hesla podle názvu.
     *
     * @param registerTypeName název
     * @return typ rejstříkového hesla
     */
    RegRegisterType findRegisterTypeByName(String registerTypeName);
}
