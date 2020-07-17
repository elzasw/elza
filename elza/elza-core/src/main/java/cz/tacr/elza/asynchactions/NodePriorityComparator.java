package cz.tacr.elza.asynchactions;

import java.util.Comparator;

/**
 * Řazení požadavků podle priority a ID požadavku.
 */
public class NodePriorityComparator implements Comparator<AsyncRequest> {

    @Override
    public int compare(AsyncRequest r1, AsyncRequest r2) {
        if (r1.getPriority().equals(r2.getPriority())) {
            return r1.getRequestId().compareTo(r2.getRequestId());
        } else {
            return r2.getPriority().compareTo(r1.getPriority());
        }
    }
}
