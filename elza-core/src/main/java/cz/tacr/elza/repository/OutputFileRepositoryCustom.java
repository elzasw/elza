package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;


/**
 * ArrOutputFile repository - custom - doplnění vyhledávání
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
public interface OutputFileRepositoryCustom {

    FilteredResult<ArrOutputFile> findByTextAndResult(String search, ArrOutputResult result, Integer firstResult, Integer maxResults);
}
