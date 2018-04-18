package cz.tacr.elza.repository;


import cz.tacr.elza.domain.ApAccessPoint;

import javax.annotation.Nullable;
import java.util.*;


/**
 * Metody pro samostatnou implementaci repository {@link ApAccessPointRepository}.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ApAccessPointRepositoryCustom {

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (record, charateristics, comment),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     * @param searchRecord      hledaný řetězec, může být null
     * @param apTypeIds   typ záznamu
     * @param firstResult       index prvního záznamu, začíná od 0
     * @param maxResults        počet výsledků k vrácení
     * @param scopeIdsForSearch id tříd, do který spadají rejstříky
     */
    List<ApAccessPoint> findApAccessPointByTextAndType(@Nullable String searchRecord,
                                                  @Nullable Collection<Integer> apTypeIds,
                                                  Integer firstResult,
                                                  Integer maxResults,
                                                  Set<Integer> scopeIdsForSearch,
                                                  Boolean excludeInvalid);


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findApAccessPointByTextAndType(String, Collection, Integer, Integer, ApAccessPoint, Set, Boolean)}
     * @param searchRecord    hledaný řetězec, může být null
     * @param apTypeIds typ záznamu
     * @param parentRecord    nadřazený rejstřík, může být null
     * @param scopeIds        id tříd, do který spadají rejstříky
     * @param excludeInvalid
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    long findApAccessPointByTextAndTypeCount(String searchRecord, Collection<Integer> apTypeIds, Set<Integer> scopeIds, boolean excludeInvalid);
}
