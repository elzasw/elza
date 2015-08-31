package cz.tacr.elza.api.controller;

import cz.tacr.elza.api.RegExternalSource;
import cz.tacr.elza.api.RegRecord;
import cz.tacr.elza.api.RegRegisterType;

import java.util.List;


/**
 * Rozhraní operací pro rejstřík.
 */
public interface RegistryManager<RR extends RegRecord> {

    /**
     * Vytvoření nového záznamu.
     *
     * @param regRecord         naplněný objekt, bez vazeb
     * @param registerTypeId    id typu rejstříku
     * @param externalSourceId  id externího zdroje, může být null
     * @return              nově vytvořený objekt
     */
    RR createRecord(RR regRecord, Integer registerTypeId, Integer externalSourceId);

    /**
     * Update záznamu.
     *
     * @param record            naplněný objekt s vlastním ID, bez vazeb
     * @param registerTypeId    id typu rejstříku
     * @param externalSourceId  id externího zdroje, může být null
     * @return                  změněný objekt
     */
    RegRecord updateRecord(RR record, Integer registerTypeId, Integer externalSourceId);

    /**
     * Smaže entity které používají daný záznam a pak záznam samotný.
     *
     * @param recordId  id záznamu rejstříku
     */
    void deleteRecord(Integer recordId);

    /**
     * Smaže variantní záznam.
     *
     * @param variantRecordId       id variantního záznamu
     */
    void deleteVariantRecord(Integer variantRecordId);

    /**
     * @return  vrátí seznam typů registrů
     */
    List<? extends RegRegisterType> getRegisterTypes();

    /**
     * @return  vrátí seznam externích zdrojů rejstříkových hesel
     */
    List<? extends RegExternalSource> getExternalSources();

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (record, charateristics, comment),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param search            hledaný řetězec, může být null
     * @param registerTypeId    typ záznamu
     * @param from              index prvního záznamu, začíná od 0
     * @param count             počet výsledků k vrácení
     * @return                  vybrané záznamy dle popisu seřazené za record, nebo prázdná množina
     */
    List<? extends RegRecord> findRecord(String search, Integer from, Integer count, Integer registerTypeId);

    /**
     * Celkový počet záznamů v DB pro funkci {@link #findRecord(String, Integer, Integer, Integer)}
     *
     * @param search            hledaný řetězec, může být null
     * @param registerTypeId    typ záznamu
     * @return                  celkový počet záznamů, který je v db za dané parametry
     */
    long findRecordCount(String search, Integer registerTypeId);

    /**
     * Vrátí jeden záznam dle id.
     * @param recordId      id požadovaného záznamu
     * @return              záznam
     */
    RegRecord getRecord(Integer recordId);

}
