package cz.tacr.elza.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Iterátor kolekcí. Vrací podkolekci s danou velikostí.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 08.01.2016
 */
public class ObjectListIterator<T> {

    /**
     * Maximální velikost jedné vrácené kolekce.
     */
    public static final int MAXIMAL_ITERATION_SIZE = 1500;


    private List<T> list;
    private int maximalIterationSize = MAXIMAL_ITERATION_SIZE;
    private int index = 0;

    public ObjectListIterator(final Collection<T> list) {
        this.list = new ArrayList<>(list);
    }

    public ObjectListIterator(final int maximalIterationSize, final Collection<T> list) {
        this.maximalIterationSize = maximalIterationSize;
        this.list = new ArrayList<>(list);
    }

    public boolean hasNext() {
        return this.index < this.list.size();
    }

    /**
     * Vrátí další podkolekci.
     *
     * @return podkolekce
     */
    public List<T> next() {
        int size = Math.min(this.maximalIterationSize, this.list.size() - this.index);
        List<T> result = new ArrayList<>(size);
        result.addAll(this.list.subList(this.index, this.index + size));
        this.index += this.maximalIterationSize;
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException("Remove opreration is not supported.");
    }
}
