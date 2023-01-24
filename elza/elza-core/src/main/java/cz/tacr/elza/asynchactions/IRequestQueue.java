package cz.tacr.elza.asynchactions;

import java.util.Collection;
import java.util.List;

public interface IRequestQueue<E> extends Iterable<E> {

    int size();

    boolean isEmpty();

    boolean add(final E e);

    boolean remove(final E o);

    boolean addAll(final Collection<? extends E> c);

    void clear();

    List<E> poll();

    /**
     * Vyhledání položky podle identifikátoru návazné entity.
     *
     * @param id identifikátor návazné entity fronty
     * @return nalezená položka
     */
    E findById(final Integer id);
}
