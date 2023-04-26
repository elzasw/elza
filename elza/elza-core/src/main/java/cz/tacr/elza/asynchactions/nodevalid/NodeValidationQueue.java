package cz.tacr.elza.asynchactions.nodevalid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;

import cz.tacr.elza.asynchactions.IAsyncRequest;
import cz.tacr.elza.asynchactions.IRequestQueue;

public class NodeValidationQueue implements IRequestQueue<IAsyncRequest> {

    private static int BATCH_SIZE = 100;

    private static int INITIAL_QUEUE_SIZE = 100;

    private static final Comparator<IAsyncRequest> comparator = new NodePriorityComparator();

    private static final Comparator<NodeQueue> nodeQueuePriorityComparator = new NodeQueuePriorityComparator();

    /**
     * Queue with queues per fond
     */
    private final Queue<NodeQueue> queue = new PriorityQueue<>(INITIAL_QUEUE_SIZE,
            nodeQueuePriorityComparator);

    // Lookups
    private final Map<Integer, IAsyncRequest> idMap = new HashMap<>();
    private final Map<Integer, NodeQueue> fundVersionMap = new HashMap<>();

    /**
     *
     */
    public NodeValidationQueue() {
    }

    @Override
    public int size() {
        int size = 0;
        for (NodeQueue nodeQueue : queue) {
            size += nodeQueue.size();
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean add(final IAsyncRequest request) {
        NodeQueue nodeQueue = findQueueByFundVersionId(request);
        if (nodeQueue == null) {
            // Prepare new queue
            nodeQueue = NodeQueue.of(new PriorityQueue<>(INITIAL_QUEUE_SIZE, comparator),
                                     request.getFundVersionId());
            // queue should not be empty before adding
            // it allows to put queue on right position
            nodeQueue.add(request);

            fundVersionMap.put(request.getFundVersionId(), nodeQueue);
            // Add to the master queue, after first element inserted
            queue.add(nodeQueue);
        } else {
            nodeQueue.add(request);
        }
        idMap.put(request.getCurrentId(), request);

        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends IAsyncRequest> c) {
        if (CollectionUtils.isNotEmpty(c)) {
            for (IAsyncRequest e : c) {
                add(e);
            }
        }
        return true;
    }

    @Override
    public boolean remove(final IAsyncRequest e) {
        NodeQueue nodeQueue = findQueueByFundVersionId(e);
        if (nodeQueue == null) {
            return false;
        }
        idMap.remove(e.getCurrentId());

        boolean remove = nodeQueue.remove(e);
        if (nodeQueue.isEmpty()) {
            queue.remove(nodeQueue);
            fundVersionMap.remove(nodeQueue.getFundVersion());
        }
        return remove;
    }

    @Override
    public void clear() {
        queue.clear();
        fundVersionMap.clear();
        idMap.clear();
    }

    @Override
    public List<IAsyncRequest> poll() {
        NodeQueue nodeQueue = queue.poll();
        if (nodeQueue == null) {
            return null;
        }

        List<IAsyncRequest> result;
        if (isFirstFailed(nodeQueue)) {
            // handle failure
            IAsyncRequest item = nodeQueue.poll();
            idMap.remove(item.getCurrentId());

            result = Collections.singletonList(item);
        } else {
            // response list
            result = new ArrayList<>(BATCH_SIZE);
            int i = 0;
            while (i < BATCH_SIZE && !nodeQueue.isEmpty()) {
                IAsyncRequest item = nodeQueue.poll();
                result.add(item);
                idMap.remove(item.getCurrentId());
                i++;
            }
        }

        if (nodeQueue.isEmpty()) {
            queue.remove(nodeQueue);
            fundVersionMap.remove(nodeQueue.getFundVersion());
        }
        return result;
    }

    private boolean isFirstFailed(NodeQueue nodeQueue) {
        IAsyncRequest item = nodeQueue.peak();
        NodeValidationRequest nvr = (NodeValidationRequest) item;
        return nvr.isFailed();
    }

    @Override
    public IAsyncRequest findById(Integer id) {
        return idMap.get(id);
    }

    private NodeQueue findQueueByFundVersionId(IAsyncRequest e) {
        return fundVersionMap.get(e.getFundVersionId());
    }

    @Override
    public Iterator<IAsyncRequest> iterator() {
        return new Iterator<IAsyncRequest>() {
            private final Iterator<IAsyncRequest> it = idMap.values().iterator();
            private IAsyncRequest lastNext;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public IAsyncRequest next() {
                lastNext = it.next();
                return lastNext;
            }

            @Override
            public void remove() {
                if (lastNext != null) {
                    NodeQueue nodeQueue = findQueueByFundVersionId(lastNext);
                    if (nodeQueue != null) {
                        boolean remove = nodeQueue.remove(lastNext);
                        if (nodeQueue.isEmpty()) {
                            queue.remove(nodeQueue);
                            fundVersionMap.remove(nodeQueue.getFundVersion());
                        }
                    }
                }
                it.remove();
            }
        };
    }

    @Override
    public void forEach(final Consumer<? super IAsyncRequest> action) {
        for (NodeQueue nodeQueue : queue) {
            nodeQueue.forEach(action);
        }
    }

    @Override
    public Spliterator<IAsyncRequest> spliterator() {
        throw new NotImplementedException("spliterator not implemented");
    }
}
