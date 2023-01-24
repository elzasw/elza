package cz.tacr.elza.asynchactions;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;

public class NodeQueue<E> implements Iterable<E> {

    private final Queue<E> originalQueue;
    private final Integer fundVersion;

    private NodeQueue(final Queue<E> originalQueue,
                      final Integer fundVersion) {
        Validate.notNull(originalQueue);
        Validate.notNull(fundVersion);
        this.originalQueue = originalQueue;
        this.fundVersion = fundVersion;
    }

    /**
     *
     *
     * @param queue původní implnentace fronty
     * @param fundVersion id verze fund
     * @param <E> generičnost
     * @return obalená fronta
     */
    public static <E> NodeQueue<E> of(final Queue<E> queue,
                                      final Integer fundVersion) {
        return new NodeQueue<>(queue, fundVersion);
    }

    public int size() {
        return originalQueue.size();
    }

    public boolean isEmpty() {
        return originalQueue.isEmpty();
    }

    public boolean add(final E e) {
        return originalQueue.add(e);
    }

    public boolean remove(final E o) {
        return originalQueue.remove(o);
    }

    public boolean addAll(final Collection<? extends E> c) {
        return originalQueue.addAll(c);
    }

    public void clear() {
        originalQueue.clear();
    }

    public E poll() {
        return originalQueue.poll();
    }

    public E peak() {
        return originalQueue.peek();
    }

    public Integer getFundVersion() {
        return fundVersion;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<E> it = originalQueue.iterator();
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
                it.remove();
            }
        };
    }

    @Override
    public void forEach(final Consumer<? super E> action) {
        originalQueue.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        throw new NotImplementedException("spliterator not implemented");
    }
}
