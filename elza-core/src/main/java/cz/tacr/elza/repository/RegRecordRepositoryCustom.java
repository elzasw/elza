package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.RegRecord;


/**
 * Metody pro samostatnou implementaci repository {@link RegRecordRepository}.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegRecordRepositoryCustom {

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (record, charateristics, comment),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param searchRecord      hledaný řetězec, může být null
     * @param registerTypeIds   typ záznamu
     * @param firstResult       index prvního záznamu, začíná od 0
     * @param maxResults        počet výsledků k vrácení
     * @param scopeIdsForRecord id tříd, do který spadají rejstříky
     * @param scopeIdsForRecord @return                  vybrané záznamy dle popisu seřazené za record, nbeo prázdná
     *                          množina
     */
    List<RegRecord> findRegRecordByTextAndType(@Nullable String searchRecord,
                                               @Nullable Collection<Integer> registerTypeIds,
                                               Integer firstResult,
                                               Integer maxResults,
                                               RegRecord parentRecord,
                                               Set<Integer> scopeIdsForRecord);


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findRegRecordByTextAndType(String, Collection, Integer, Integer,
     * RegRecord, Set)}
     *
     * @param searchRecord    hledaný řetězec, může být null
     * @param registerTypeIds typ záznamu
     * @param parentRecord    nadřazený rejstřík, může být null
     * @param scopeIds        id tříd, do který spadají rejstříky
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    long findRegRecordByTextAndTypeCount(String searchRecord, Collection<Integer> registerTypeIds,
                                         @Nullable RegRecord parentRecord, Set<Integer> scopeIds);


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findRootRecords(Collection, Integer, Integer, Set)}
     *
     * @param registerTypeIds   typ záznamu
     * @param scopeIdsForRecord id tříd, do který spadají rejstříky
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    long findRootRecordsByTypeCount(Collection<Integer> registerTypeIds,
                                    final Set<Integer> scopeIdsForRecord);


    /**
     * Nalezne kořenové záznamy rejstříku, které mají daný typ.
     *
     * @param registerTypeIds   typ záznamu
     * @param firstResult       index prvního záznamu, začíná od 0
     * @param maxResults        počet výsledků k vrácení
     * @param scopeIdsForRecord id tříd, do který spadají rejstříky
     * @return vybrané záznamy dle popisu seřazené za record, nebo prázdná množina
     */
    List<RegRecord> findRootRecords(@Nullable Collection<Integer> registerTypeIds,
                                    Integer firstResult,
                                    Integer maxResults,
                                    final Set<Integer> scopeIdsForRecord);
}
