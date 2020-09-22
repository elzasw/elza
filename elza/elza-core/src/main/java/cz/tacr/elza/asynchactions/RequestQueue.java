package cz.tacr.elza.asynchactions;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Zaobalovací třída pro frontu požadavků. Umožňuje rychlý přístup k položce podle identifikátoru, který lze
 * napočítávat funkcí {@link RequestQueue#calcId}.
 *
 * @param <E> generičnost
 */
public class RequestQueue<E> implements Iterable<E> {

    private final Queue<E> originalQueue;
    private final Function<E, Integer> calcId;
    private final Map<Integer, E> idMap = new HashMap<>();

    private RequestQueue(final Queue<E> originalQueue,
                         final Function<E, Integer> calcId) {
        Validate.notNull(originalQueue);
        Validate.notNull(calcId);
        this.originalQueue = originalQueue;
        this.calcId = calcId;
        init();
    }

    private RequestQueue(final Queue<E> originalQueue) {
        Validate.notNull(originalQueue);
        this.originalQueue = originalQueue;
        this.calcId = null;
    }

    private void init() {
        if (calcId != null && CollectionUtils.isNotEmpty(originalQueue)) {
            for (E e : originalQueue) {
                idMap.put(calcId.apply(e), e);
            }
        }
    }

    /**
     * Rozšířené obalení fronty požadavků - možnost rychlého přístupu k položce fronty podle identifikátoru metodou
     * {@link RequestQueue#findById(java.lang.Integer)}
     *
     * @param queue původní implnentace fronty
     * @param calcId funkce pro nápočet identifikátoru
     * @param <E> generičnost
     * @return obalená fronta
     */
    public static <E> RequestQueue<E> of(final Queue<E> queue,
                                         final Function<E, Integer> calcId) {
        return new RequestQueue<>(queue, calcId);
    }

    /**
     * Standartní obalení fronty požadavků.
     *
     * @param queue původní implnentace fronty
     * @param <E> generičnost
     * @return obalená fronta
     */
    public static <E> RequestQueue<E> of(final Queue<E> queue) {
        return new RequestQueue<>(queue);
    }

    public int size() {
        return originalQueue.size();
    }

    public boolean isEmpty() {
        return originalQueue.isEmpty();
    }

    public boolean add(final E e) {
        if (calcId != null) {
            idMap.put(calcId.apply(e), e);
        }
        return originalQueue.add(e);
    }

    public boolean remove(final E o) {
        if (calcId != null) {
            idMap.remove(calcId.apply(o));
        }
        return originalQueue.remove(o);
    }

    public boolean addAll(final Collection<? extends E> c) {
        if (calcId != null) {
            for (E e : c) {
                idMap.put(calcId.apply(e), e);
            }
        }
        return originalQueue.addAll(c);
    }

    public void clear() {
        originalQueue.clear();
        idMap.clear();
    }

    public E poll() {
        E item = originalQueue.poll();
        if (calcId != null && item != null) {
            idMap.remove(calcId.apply(item));
        }
        return item;
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
                if (calcId != null && lastNext != null) {
                    idMap.remove(calcId.apply(lastNext));
                }
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

    /**
     * Vyhledání položky podle identifikátoru návazné entity.
     *
     * @param id identifikátor návazné entity fronty
     * @return nalezená položka
     */
    public E findById(final Integer id) {
        if (calcId != null) {
            return idMap.get(id);
        } else {
            throw new IllegalStateException("not defined calcId, method not support");
        }
    }
}
