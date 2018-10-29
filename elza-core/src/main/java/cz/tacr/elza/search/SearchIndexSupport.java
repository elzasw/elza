package cz.tacr.elza.search;

import java.util.Collection;
import java.util.Map;

/**
 * Hibernate Search support
 */
public interface SearchIndexSupport<T> {

    /**
     * Seznam objektu pro preindexovani v Hibernate Search.
     *
     * @param ids seznam ID
     * @return seznam objektu (ID -> entita)
     */
    Map<Integer, T> findToIndex(Collection<Integer> ids);
}
