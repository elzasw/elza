package cz.tacr.elza.asynchactions.nodevalid;

import java.util.Comparator;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.asynchactions.IAsyncRequest;

/**
 * Řazení požadavků podle priority a ID požadavku.
 */
public class NodeQueuePriorityComparator implements Comparator<NodeQueue> {

    @Override
    public int compare(NodeQueue n1, NodeQueue n2) {
        IAsyncRequest r1 = n1.peak();
        IAsyncRequest r2 = n2.peak();
        // one of queues might be empty
        if (r1 == null || r2 == null) {
            // empty queues cannot be ordered properly
            // simply compare fundVersionId
            return n1.getFundVersion().compareTo(n2.getFundVersion());
        }
        Integer priotiry1 = r1.getPriority();
        Integer priotiry2 = r2.getPriority();
        if (priotiry1 == null || priotiry2 == null) {
            Validate.isTrue(false);
        }
        if (priotiry1.equals(priotiry2)) {
            return r1.getRequestId().compareTo(r2.getRequestId());
        } else {
            return priotiry2.compareTo(priotiry1);
        }
    }
}
