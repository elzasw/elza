package cz.tacr.elza.common;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;


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

    public static <T> void forEachPage(final Collection<T> list, Consumer<Collection<T>> call) {
        if (CollectionUtils.isNotEmpty(list)) {
            ObjectListIterator<T> iterator = new ObjectListIterator<>(list);
            while (iterator.hasNext()) {
                call.accept(iterator.next());
            }
        }
    }

    public static <T, R> List<R> findIterable(final Collection<T> list, Function<Collection<T>, List<R>> call) {
        List<R> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(list)) {
            ObjectListIterator<T> iterator = new ObjectListIterator<>(list);
            while (iterator.hasNext()) {
                result.addAll(call.apply(iterator.next()));
            }
        }
        return result;
    }

    public static <T, R> Set<R> findIterableSet(final Collection<T> list, Function<Collection<T>, Collection<R>> call) {
        Set<R> result = new HashSet<>();
        if (CollectionUtils.isNotEmpty(list)) {
            ObjectListIterator<T> iterator = new ObjectListIterator<>(list);
            while (iterator.hasNext()) {
                result.addAll(call.apply(iterator.next()));
            }
        }
        return result;
    }
}
