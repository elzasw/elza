package cz.tacr.elza.asynchactions.nodevalid;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.asynchactions.IAsyncRequest;

public class NodeQueue implements Iterable<IAsyncRequest> {

    private final Queue<IAsyncRequest> queue;
    private final Integer fundVersionId;

    public NodeQueue(final Queue<IAsyncRequest> originalQueue,
                     final Integer fundVersionId) {
        Validate.notNull(originalQueue);
        Validate.notNull(fundVersionId);
        this.queue = originalQueue;
        this.fundVersionId = fundVersionId;
    }

    /**
     *
     *
     * @param queue
     *            implementace fronty
     * @param fundVersion
     *            id verze fund
     * @param <E>
     *            generičnost
     * @return obalená fronta
     */
    public static NodeQueue of(final Queue<IAsyncRequest> queue,
                               final Integer fundVersion) {
        return new NodeQueue(queue, fundVersion);
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean add(final IAsyncRequest request) {
        return queue.add(request);
    }

    public boolean remove(final IAsyncRequest o) {
        return queue.remove(o);
    }

    public boolean addAll(final Collection<? extends IAsyncRequest> c) {
        return queue.addAll(c);
    }

    public void clear() {
        queue.clear();
    }

    public IAsyncRequest poll() {
        return queue.poll();
    }

    public IAsyncRequest peak() {
        return queue.peek();
    }

    public Integer getFundVersion() {
        return fundVersionId;
    }

    @Override
    public Iterator<IAsyncRequest> iterator() {
        return new Iterator<IAsyncRequest>() {
            private final Iterator<IAsyncRequest> it = queue.iterator();
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
                it.remove();
            }
        };
    }

    @Override
    public void forEach(final Consumer<? super IAsyncRequest> action) {
        queue.forEach(action);
    }

    @Override
    public Spliterator<IAsyncRequest> spliterator() {
        throw new NotImplementedException("spliterator not implemented");
    }
}
