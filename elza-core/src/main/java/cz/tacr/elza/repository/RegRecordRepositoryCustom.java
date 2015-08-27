package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegRecord;

import javax.annotation.Nullable;
import java.util.List;

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
     * @param registerTypeId    ty záznamu
     * @param firstResult       index prvního záznamu, začíná od 0
     * @param maxResults        počet výsledků k vrácení
     * @return                  vybrané záznamy dle popisu seřazené za record, nbeo prázdná množina
     */
    List<RegRecord> findRegRecordByTextAndType(@Nullable String searchRecord, Integer registerTypeId,
                                               Integer firstResult, Integer maxResults);
}
