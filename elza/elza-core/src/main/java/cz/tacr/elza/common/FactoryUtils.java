package cz.tacr.elza.common;

import java.util.*;
import java.util.function.Function;

/**
 * Factory utils for basic support.
 */
public class FactoryUtils {

    public static <S, T> List<T> transformList(final Collection<S> src, final Function<S, T> transform) {
        if (src == null) {
            return null;
        }
        int size = src.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        List<T> target = new ArrayList<>(src.size());
        for (S srcItem : src) {
            T targetItem = transform.apply(srcItem);
            target.add(targetItem);
        }
        return target;
    }

    public static <S, T, U> Map<U, T> transformMap(final Collection<S> src, final Function<S, U> transformKey, final Function<S, T> transformValue) {
        if (src == null) {
            return null;
        }
        int size = src.size();
        if (size == 0) {
            return Collections.emptyMap();
        }
        Map<U, T> target = new HashMap<>(src.size());
        for (S srcItem : src) {
            T targetItem = transformValue.apply(srcItem);
            U targetId = transformKey.apply(srcItem);
            target.put(targetId, targetItem);
        }
        return target;
    }

}
