package cz.tacr.elza.repository;


import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import cz.tacr.elza.core.data.SearchType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;


/**
 * Metody pro samostatnou implementaci repository {@link ApAccessPointRepository}.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ApAccessPointRepositoryCustom {

    /**
     * Nalezne takové záznamy rejstříku, které mají daný typ a jejich textová pole (record, charateristics, comment),
     * nebo pole variantního záznamu obsahují hledaný řetězec. V případě, že hledaný řetězec je null, nevyhodnocuje se.
     *
     * @param searchRecord hledaný řetězec, může být null
     * @param apTypeIds typ záznamu
     * @param firstResult index prvního záznamu, začíná od 0
     * @param maxResults počet výsledků k vrácení
     * @param scopeIds id tříd, do který spadají rejstříky
     */
    List<ApState> findApAccessPointByTextAndType(
            @Nullable String searchRecord,
            @Nullable Collection<Integer> apTypeIds,
            Integer firstResult,
            Integer maxResults,
            @Nullable Set<Integer> scopeIds,
            @Nullable Collection<ApState.StateApproval> approvalStates,
            @Nullable SearchType searchTypeName,
            @Nullable SearchType searchTypeUsername);


    /**
     * Celkový počet záznamů v DB pro funkci {@link #findApAccessPointByTextAndType(String, Collection, Integer, Integer, ApAccessPoint, Set, Boolean)}
     *
     * @param searchRecord hledaný řetězec, může být null
     * @param apTypeIds typ záznamu
     * @param parentRecord nadřazený rejstřík, může být null
     * @param scopeIds id tříd, do který spadají rejstříky
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    long findApAccessPointByTextAndTypeCount(
            @Nullable String searchRecord,
            @Nullable Collection<Integer> apTypeIds,
            @Nullable Set<Integer> scopeIds,
            @Nullable Collection<ApState.StateApproval> approvalStates,
            @Nullable SearchType searchTypeName,
            @Nullable SearchType searchTypeUsername);

    List<ApAccessPoint> findAccessPointsBySinglePartValues(List<Object> criterias);
}
