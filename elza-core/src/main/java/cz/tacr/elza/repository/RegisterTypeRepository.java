package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.RegRegisterType;


/**
 * Repository pro typ záznamu rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface RegisterTypeRepository extends JpaRepository<RegRegisterType, Integer>, RegisterTypeRepositoryCustom {

    /**
     * Najde všechny typy rejstříkových hesel, které jsou napojeny na typy osob.
     *
     * @return seznam typů rejstříkových hesel
     */
    @Query("SELECT r FROM reg_register_type r WHERE r.partyType IS NOT null")
    List<RegRegisterType> findTypesForPartyTypes();


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
    @Query("SELECT t FROM reg_register_type t WHERE t.partyType = ?1 AND t.addRecord = true ORDER BY t.code ASC")
    List<RegRegisterType> findByPartyTypeEnableAdding(ParPartyType partyType);


    /**
     * Hledá typy, které mají true pro přidání záznamu a nejsou pro typ osob.
     *
     * @return seznam typů
     */
    @Query("SELECT t FROM reg_register_type t WHERE t.partyType IS NULL AND t.addRecord = true ORDER BY t.code ASC")
    List<RegRegisterType> findNullPartyTypeEnableAdding();


    /**
     * Najde typy rejstříků napojené na typ vztahu.
     *
     * @param relationRoleType typ vztahu
     * @return typy rejstříků
     */
    @Query("SELECT rr.registerType FROM par_registry_role rr WHERE rr.roleType = ?1")
    List<RegRegisterType> findByRelationRoleType(ParRelationRoleType relationRoleType);
}
