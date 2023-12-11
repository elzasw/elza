package cz.tacr.elza.repository;

import cz.tacr.elza.domain.DmsFile;
import jakarta.annotation.Nullable;


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
    FilteredResult<DmsFile> findByText(final @Nullable String searchRecord, final Integer firstResult, final Integer maxResults);

}
