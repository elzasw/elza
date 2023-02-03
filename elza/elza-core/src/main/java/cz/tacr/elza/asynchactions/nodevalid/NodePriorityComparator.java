package cz.tacr.elza.asynchactions.nodevalid;

import java.util.Comparator;

import cz.tacr.elza.asynchactions.IAsyncRequest;

/**
 * Řazení požadavků podle priority a ID požadavku.
 */
public class NodePriorityComparator implements Comparator<IAsyncRequest> {

    @Override
    public int compare(IAsyncRequest r1, IAsyncRequest r2) {
        if (r1.getPriority() == r2.getPriority()) {
            return r1.getRequestId().compareTo(r2.getRequestId());
        } else {
            return (r2.getPriority() < r1.getPriority()) ? -1 : 1;
        }
    }
}
