package cz.tacr.elza.asynchactions;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class AsQueue<E> implements IRequestQueue<E> {

    private static int BATCH_SIZE = 100;

    private final Queue<NodeQueue<E>> originalQueue;
    private final Function<E, Integer> calcId;
    private final Map<Integer, E> idMap = new HashMap<>();
    private final Function<E, Integer> fundId;
    private final Map<Integer, NodeQueue<E>> fundMap = new HashMap<>();
    private final Comparator<E> comparator;
    private final Function<E, Boolean> failed;

    private AsQueue(final Queue<NodeQueue<E>> originalQueue,
                    final Function<E, Integer> calcId,
                    final Function<E, Integer> fundId,
                    final Function<E, Boolean> failed,
                    final Comparator<E> comparator) {
        Validate.notNull(originalQueue);
        Validate.notNull(calcId);
        Validate.notNull(fundId);
        Validate.notNull(comparator);
        Validate.notNull(failed);
        this.originalQueue = originalQueue;
        this.calcId = calcId;
        this.fundId = fundId;
        this.failed = failed;
        this.comparator = comparator;
    }

    /**
     *
     *
     * @param queue původní implnentace fronty
     * @param fundId funkce pro získání id verze fund
     * @param <E> generičnost
     * @return obalená fronta
     */
    public static <E> AsQueue<E> of(final Queue<NodeQueue<E>> queue,
                                    final Function<E, Integer> calcId,
                                    final Function<E, Integer> fundId,
                                    final Function<E, Boolean> failed,
                                    final Comparator<E> comparator) {
        return new AsQueue<>(queue, calcId, fundId, failed, comparator);
    }

    @Override
    public int size() {
        int size = 0;
        for (NodeQueue<E> nodeQueue : originalQueue) {
            size += nodeQueue.size();
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return originalQueue.isEmpty();
    }

    @Override
    public boolean add(final E e) {
        NodeQueue<E> nodeQueue = findQueueByFundId(e);
        if (nodeQueue == null) {
            nodeQueue = NodeQueue.of(new PriorityQueue<>(1000, comparator), fundId.apply(e));
            fundMap.put(fundId.apply(e), nodeQueue);
            originalQueue.add(nodeQueue);
        }
        if (calcId != null) {
            idMap.put(calcId.apply(e), e);
        }
        return nodeQueue.add(e);
    }

    @Override
    public boolean remove(final E e) {
        NodeQueue<E> nodeQueue = findQueueByFundId(e);
        if (nodeQueue == null) {
            return false;
        }
        if (calcId != null) {
            idMap.remove(calcId.apply(e));
        }
        boolean remove = nodeQueue.remove(e);
        if (nodeQueue.isEmpty()) {
            originalQueue.remove(nodeQueue);
            fundMap.remove(nodeQueue.getFundVersion());
        }
        return remove;
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        if (CollectionUtils.isNotEmpty(c)) {
            for (E e : c) {
                if (calcId != null) {
                    idMap.put(calcId.apply(e), e);
                }
                NodeQueue<E> nodeQueue = findQueueByFundId(e);
                if (nodeQueue == null) {
                    nodeQueue = NodeQueue.of(new PriorityQueue<>(1000, comparator), fundId.apply(e));
                    fundMap.put(fundId.apply(e), nodeQueue);
                    originalQueue.add(nodeQueue);
                }
                nodeQueue.add(e);
            }
        }

        return true;
    }

    @Override
    public void clear() {
        originalQueue.clear();
        fundMap.clear();
        idMap.clear();
    }

    @Override
    public List<E> poll() {
        NodeQueue<E> nodeQueue = originalQueue.poll();
        List<E> l = new ArrayList<>();
        int i = 0;
        if (nodeQueue != null) {
            while (i < BATCH_SIZE && !nodeQueue.isEmpty()) {
                if (isFirstFailed(nodeQueue)) {
                    if (l.size() == 0) {
                        E item = nodeQueue.poll();
                        l.add(item);
                        if (calcId != null && item != null) {
                            idMap.remove(calcId.apply(item));
                        }
                    }
                    break;
                }
                E item = nodeQueue.poll();
                l.add(item);
                if (calcId != null && item != null) {
                    idMap.remove(calcId.apply(item));
                }
                i++;
            }

            if (nodeQueue.isEmpty()) {
                originalQueue.remove(nodeQueue);
                fundMap.remove(nodeQueue.getFundVersion());
            }
        }
        return l;
    }

    private boolean isFirstFailed(NodeQueue<E> nodeQueue) {
        E item = nodeQueue.peak();
        return failed.apply(item);
    }

    @Override
    public E findById(Integer id) {
        if (calcId != null) {
            return idMap.get(id);
        } else {
            throw new IllegalStateException("not defined calcId, method not support");
        }
    }

    private NodeQueue<E> findQueueByFundId(E e) {
        return fundMap.get(fundId.apply(e));
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<E> it = idMap.values().iterator();
            private E lastNext;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                lastNext = it.next();
                return lastNext;
            }

            @Override
            public void remove() {
                if (lastNext != null) {
                    NodeQueue<E> nodeQueue = findQueueByFundId(lastNext);
                    if (nodeQueue != null) {
                        boolean remove = nodeQueue.remove(lastNext);
                        if (nodeQueue.isEmpty()) {
                            originalQueue.remove(nodeQueue);
                            fundMap.remove(nodeQueue.getFundVersion());
                        }
                    }
                }
                it.remove();
            }
        };
    }

    @Override
    public void forEach(final Consumer<? super E> action) {
        for (NodeQueue<E> nodeQueue : originalQueue) {
            nodeQueue.forEach(action);
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        throw new NotImplementedException("spliterator not implemented");
    }
}
