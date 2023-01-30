package cz.tacr.elza.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;


/**
 * Iterátor kolekcí. Vrací podkolekci s danou velikostí.
 *
 * @since 08.01.2016
 */
public class ObjectListIterator<T> implements Iterator<List<T>> {

    /**
     * Maximální velikost jedné vrácené kolekce.
     */
    private static int maxIterationSize = 1500;

    private List<T> list;
    private int maximalIterationSize = maxIterationSize;
    private int index = 0;

    public ObjectListIterator(final Collection<T> list) {
        this.list = new ArrayList<>(list);
    }

    private ObjectListIterator(final Iterable<T> iterable) {
        // TODO: redo to not create extra list
        this.list = new ArrayList<>();
        iterable.forEach(e -> list.add(e));
    }

    public ObjectListIterator(final int maximalIterationSize, final Collection<T> list) {
        this.maximalIterationSize = maximalIterationSize;
        this.list = new ArrayList<>(list);
    }

    public static void setMaxBatchSize(int maxBatchSize) {
        maxIterationSize = maxBatchSize;
    }

    public static int getMaxBatchSize() {
        return maxIterationSize; 
    }

    @Override
    public boolean hasNext() {
        return this.index < this.list.size();
    }

    /**
     * Vrátí další podkolekci.
     *
     * @return podkolekce
     */
    @Override
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

    public static <T> void forEachPage(final Iterable<T> iter, Consumer<Collection<T>> call) {
        if (iter == null) {
            return;
        }
        ObjectListIterator<T> iterator = new ObjectListIterator<>(iter);
        while (iterator.hasNext()) {
            call.accept(iterator.next());
        }
    }

    // TODO: rename
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

    // TODO: rename
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
