package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;

import java.util.Collection;
import java.util.List;


/**
 * ArrFile repository - custom - doplnění vyhledávání
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
public interface FundFileRepositoryCustom {

    FilteredResult<ArrFile> findByTextAndFund(String search, ArrFund fund, Integer firstResult, Integer maxResults);

    List<ArrFile> findFilesBySubtreeNodeIds(Collection<Integer> nodeIds, boolean ignoreRootNode);
}
