package cz.tacr.elza.search;

import java.util.Collection;
import java.util.Map;

/**
 * @author <a href="mailto:stepan.marek@coreit.cz">Stepan Marek</a>
 */
public interface SearchIndexSupport<T> {
    Map<Integer, T> findToIndex(Collection<Integer> ids);
}
