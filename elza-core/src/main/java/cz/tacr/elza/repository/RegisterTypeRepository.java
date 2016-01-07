package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ParPartyType;
import cz.tacr.elza.domain.RegRegisterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Repository pro typ záznamu rejstříku.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Repository
public interface RegisterTypeRepository extends JpaRepository<RegRegisterType, Integer> {

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

}
