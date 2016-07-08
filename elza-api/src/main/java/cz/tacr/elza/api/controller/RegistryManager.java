package cz.tacr.elza.api.controller;

import cz.tacr.elza.api.RegExternalSource;
import cz.tacr.elza.api.RegRecord;
import cz.tacr.elza.api.RegRegisterType;
import cz.tacr.elza.api.RegVariantRecord;
import cz.tacr.elza.api.vo.RegRecordWithCount;

import java.util.List;


/**
 * Rozhraní operací pro rejstřík.
 * 
 * @author vavrejn
 *
 * @param <RR> {@link RegRecord} heslo rejstříku
 * @param <VR> {@link RegVariantRecord} variantní heslo rejstříku
 */
public interface RegistryManager<RR extends RegRecord, VR extends RegVariantRecord> {

    /**
     * Vytvoření nového hesla rejstříku.
     *
     * @param regRecord     naplněný objekt s vyplněnou vazbou minimálně na typ
     * @return              nově vytvořený objekt
     */
    RR createRecord(RR regRecord);

    /**
     * Update hesla rejstříku.
     *
     * @param record            naplněný objekt s vlastním ID a vazbou minimálně na typ
     * @return                  změněný objekt
     */
    RegRecord updateRecord(RR record);

    /**
     * Smaže entity které používají dané heslo (variantní hesla, odkazy, osoby) a pak heslo samotné.
     *
     * @param recordId  id záznamu rejstříku
     */
    void deleteRecord(Integer recordId);

    /**
     * Vytvoří nový variantní heslo rejstříku.
     *
     * @param variantRecord     vyplněný objekt var. hesla s vazbou na heslo
     * @return                  nově vytvořený objekt
     */
    RegVariantRecord createVariantRecord(VR variantRecord);

    /**
     * Upraví variantní heslo rejstříku.
     *
     * @param variantRecord     vyplněný objekt var. hesla s ID a vazbou na heslo
     * @return                  změněný objekt
     */
    RegVariantRecord updateVariantRecord(VR variantRecord);

    /**
     * Smaže variantní heslo rejstříku.
     *
     * @param variantRecordId       id variantního hesla
     */
    void deleteVariantRecord(Integer variantRecordId);

    /**
     * Vrátí seznam typů rejstříku (typů hesel).
     *
     * @return  seznam typů rejstříku (typů hesel)
     */
    List<? extends RegRegisterType> getRegisterTypes();

    /**
     * Vrátí seznam externích zdrojů rejstříkových hesel.
     *
     * @return  seznam externích zdrojů rejstříkových hesel
     */
    List<? extends RegExternalSource> getExternalSources();

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (heslo, popis, poznámka),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param search            hledaný řetězec, může být null či prázdný (pak vrací vše)
     * @param from              index prvního záznamu, začíná od 0
     * @param count             počet výsledků k vrácení
     * @param registerTypeIds   IDčka typu záznamu, může být null či prázdné (pak vrací vše)
     * @return                  vybrané záznamy dle popisu seřazené za text hesla, nebo prázdná množina
     */
    RegRecordWithCount findRecord(String search, Integer from, Integer count, Integer[] registerTypeIds);

    /**
     * Vrátí jedno heslo (s variantními hesly) dle id.
     * @param recordId      id požadovaného hesla
     * @return              heslo s vazbou na var. hesla
     */
    RegRecord getRecord(Integer recordId);

    /**
     * Vrátí záznamy patřící danému typu rejstříku podle id specifikace typů atributů archivního popisu.
     * @param descItemSpecId id specifikace typů atributů archivního popisu
     * @return záznamy patřící danému typu rejstříku.
     */
    List<? extends RegRegisterType> getRegisterTypesForDescItemSpec(Integer descItemSpecId);

}
