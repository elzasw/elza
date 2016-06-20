package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DmsFile;

import javax.annotation.Nullable;
import java.util.List;


/**
 * DmsFile repository - custom - doplnění vyhledávání
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
public interface FileRepositoryCustom {

    /**
     *
     *
     * @param searchRecord      hledaný řetězec, může být null
     * @param firstResult       index prvního záznamu, začíná od 0
     * @param maxResults        počet výsledků k vrácení
     */
    List<DmsFile> findByText(final @Nullable String searchRecord,
                             final Integer firstResult,
                             final Integer maxResults);


    /**
     * Celkový počet záznamů v DB dle heledané hodnoty
     *
     * @param searchRecord    hledaný řetězec, může být null
     * @return celkový počet záznamů, který je v db za dané parametry
     */
    long findByTextCount(final String searchRecord);


}
