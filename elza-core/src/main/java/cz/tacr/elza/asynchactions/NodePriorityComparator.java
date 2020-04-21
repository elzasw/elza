package cz.tacr.elza.asynchactions;

import java.util.Comparator;

/**
 * Řazení požadavků podle priority a ID požadavku
 */
public class NodePriorityComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
        AsyncRequestVO r1 = (AsyncRequestVO) o1;
        AsyncRequestVO r2 = (AsyncRequestVO) o2;
        if (r1.getPriority() == r2.getPriority()) {
            if (r1.getRequestId() > r2.getRequestId())
                return 1;
            else if (r1.getRequestId() < r2.getRequestId())
                return -1;
            return 0;
        } else {
            return (r2.getPriority() - r1.getPriority());
        }
    }
}
