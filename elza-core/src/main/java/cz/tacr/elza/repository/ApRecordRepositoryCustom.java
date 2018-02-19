package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.ApRecord;


/**
 * Metody pro samostatnou implementaci repository {@link ApRecordRepository}.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ApRecordRepositoryCustom {

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (record, charateristics, comment),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     * @param searchRecord      hledaný řetězec, může být null
     * @param apTypeIds   typ záznamu
     * @param firstResult       index prvního záznamu, začíná od 0
     * @param maxResults        počet výsledků k vrácení
     * @param scopeIdsForSearch id tříd, do který spadají rejstříky
     */
    List<ApRecord> findApRecordByTextAndType(@Nullable String searchRecord,
                                             @Nullable Collection<Integer> apTypeIds,
                                             Integer firstResult,
                                             Integer maxResults,
                                             ApRecord parentRecord,
                                             Set<Integer> scopeIdsForSearch,
                                             Boolean excludeInvalid);


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findApRecordByTextAndType(String, Collection, Integer, Integer, ApRecord, Set, Boolean)}
     * @param searchRecord    hledaný řetězec, může být null
     * @param apTypeIds typ záznamu
     * @param parentRecord    nadřazený rejstřík, může být null
     * @param scopeIds        id tříd, do který spadají rejstříky
     * @param excludeInvalid
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    long findApRecordByTextAndTypeCount(String searchRecord, Collection<Integer> apTypeIds,
                                        @Nullable ApRecord parentRecord, Set<Integer> scopeIds, boolean excludeInvalid);

    /**
     * Searches access points and all their parents. Parents are always returned before children.
     *
     * @param apIds collection of AP ids
     */
    List<ApRecord> findAccessPointsWithParents(Collection<Integer> apIds);
}
