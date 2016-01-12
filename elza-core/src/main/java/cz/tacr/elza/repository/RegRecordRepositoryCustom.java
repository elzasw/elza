package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

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
     * @param registerTypeIds    typ záznamu
     * @param local             null - všechny záznamy, true - pouze lokální, false - pouze globální
     * @param firstResult       index prvního záznamu, začíná od 0
     * @param maxResults        počet výsledků k vrácení
     * @param parentRecord      nadřazený rejstřík, může být null
     * @return                  vybrané záznamy dle popisu seřazené za record, nbeo prázdná množina
     */
    List<RegRecord> findRegRecordByTextAndType(@Nullable String searchRecord,
            @Nullable Collection<Integer> registerTypeIds,
            @Nullable Boolean local,
            Integer firstResult,
            Integer maxResults,
            @Nullable RegRecord parentRecord);

    /**
     * Celkový počet záznamů v DB pro funkci {@link #findRegRecordByTextAndType(String, Collection, Boolean, Integer, Integer)}
     *
     * @param searchRecord      hledaný řetězec, může být null
     * @param registerTypeIds    typ záznamu
     * @param local             null - všechny záznamy, true - pouze lokální, false - pouze globální
     * @param parentRecord      nadřazený rejstřík, může být null
     * @return                  celkový počet záznamů, který je v db za dané parametry
     */
    long findRegRecordByTextAndTypeCount(String searchRecord, Collection<Integer> registerTypeIds, @Nullable final Boolean local,
            @Nullable RegRecord parentRecord);
}
