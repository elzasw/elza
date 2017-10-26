package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.UsrUser;


/**
 * Metody pro samostatnou implementaci repository {@link RegRecordRepository}.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface RegRecordRepositoryCustom {

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (record, charateristics, comment),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     * @param searchRecord      hledaný řetězec, může být null
     * @param registerTypeIds   typ záznamu
     * @param firstResult       index prvního záznamu, začíná od 0
     * @param maxResults        počet výsledků k vrácení
     * @param scopeIdsForSearch id tříd, do který spadají rejstříky
     */
    List<RegRecord> findRegRecordByTextAndType(@Nullable String searchRecord,
                                               @Nullable Collection<Integer> registerTypeIds,
                                               Integer firstResult,
                                               Integer maxResults,
                                               RegRecord parentRecord,
                                               Set<Integer> scopeIdsForSearch);


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findRegRecordByTextAndType(String, Collection, Integer, Integer, RegRecord, Set, boolean, UsrUser, Integer)}
     * @param searchRecord    hledaný řetězec, může být null
     * @param registerTypeIds typ záznamu
     * @param parentRecord    nadřazený rejstřík, může být null
     * @param scopeIds        id tříd, do který spadají rejstříky
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    long findRegRecordByTextAndTypeCount(String searchRecord, Collection<Integer> registerTypeIds,
                                         @Nullable RegRecord parentRecord, Set<Integer> scopeIds);

    /**
     * Searches access points and all their parents. Parents are always returned before children.
     *
     * @param apIds collection of AP ids
     */
    List<RegRecord> findAccessPointsWithParents(Collection<Integer> apIds);
}
