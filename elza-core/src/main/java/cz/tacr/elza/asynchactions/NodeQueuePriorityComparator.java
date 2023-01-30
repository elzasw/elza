package cz.tacr.elza.asynchactions;

import java.util.Comparator;

/**
 * Řazení požadavků podle priority a ID požadavku.
 */
public class NodeQueuePriorityComparator implements Comparator<NodeQueue<AsyncRequest>> {

    @Override
    public int compare(NodeQueue<AsyncRequest> n1, NodeQueue<AsyncRequest> n2) {
        AsyncRequest r1 = n1.peak();
        AsyncRequest r2 = n2.peak();
        if (r1.getPriority().equals(r2.getPriority())) {
            return r1.getRequestId().compareTo(r2.getRequestId());
        } else {
            return r2.getPriority().compareTo(r1.getPriority());
        }
    }
}
