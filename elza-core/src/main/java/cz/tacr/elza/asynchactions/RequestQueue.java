package cz.tacr.elza.asynchactions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;

/**
 * Zaobalovací třída pro frontu požadavků. Umožňuje rychlý přístup k položce podle identifikátoru, který lze
 * napočítávat funkcí {@link RequestQueue#calcId}.
 *
 * @param <E> generičnost
 */
public class RequestQueue<E> implements IRequestQueue<E> {

    private final Queue<E> originalQueue = new LinkedList<>();
    private final Function<E, Integer> calcId;
    private final Map<Integer, E> idMap = new HashMap<>();

    /**
     * Rozšířené obalení fronty požadavků - možnost rychlého přístupu k položce
     * fronty podle identifikátoru metodou
     * {@link RequestQueue#findById(java.lang.Integer)}
     *
     * @param calcId
     *            funkce pro nápočet identifikátoru
     */
    public RequestQueue(final Function<E, Integer> calcId) {
        Validate.notNull(calcId);
        this.calcId = calcId;
        init();
    }

    /**
     * Standartní obalení fronty požadavků.
     *
     */
    public RequestQueue() {
        this.calcId = null;
    }

    private void init() {
        if (calcId != null && CollectionUtils.isNotEmpty(originalQueue)) {
            for (E e : originalQueue) {
                idMap.put(calcId.apply(e), e);
            }
        }
    }

    @Override
    public int size() {
        return originalQueue.size();
    }

    @Override
    public boolean isEmpty() {
        return originalQueue.isEmpty();
    }

    @Override
    public boolean add(final E e) {
        if (calcId != null) {
            idMap.put(calcId.apply(e), e);
        }
        return originalQueue.add(e);
    }

    @Override
    public boolean remove(final E o) {
        if (calcId != null) {
            idMap.remove(calcId.apply(o));
        }
        return originalQueue.remove(o);
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        if (calcId != null) {
            for (E e : c) {
                idMap.put(calcId.apply(e), e);
            }
        }
        return originalQueue.addAll(c);
    }

    @Override
    public void clear() {
        originalQueue.clear();
        idMap.clear();
    }

    @Override
    public List<E> poll() {
        E item = originalQueue.poll();
        if (item == null) {
            return null;
        }
        if (calcId != null) {
            idMap.remove(calcId.apply(item));
        }
        return Collections.singletonList(item);
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


    @Override
    public E findById(final Integer id) {
        if (calcId != null) {
            return idMap.get(id);
        } else {
            throw new IllegalStateException("not defined calcId, method not support");
        }
    }
}
