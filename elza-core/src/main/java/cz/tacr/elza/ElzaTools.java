package cz.tacr.elza;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Obecné pomocné metody.
 *
 * @author vavrejn
 *
 */
public class ElzaTools {

    /**
     * vytvoří z listu mapu listů zagrupovanou podle zadaného klíče.
     *
     * @param <T> typ zpracovávaného objektu.
     * @param findItemConstList list pro grupovani
     * @param function - funkce vracejici hodnotu klíčš pro grupovani.
     * @return výsledná mapa.
     */
    public static <T> Map<Integer, List<T>> createGroupMap(final List<T> findItemConstList,
            final Function<T, Integer> function) {
        Map<Integer, List<T>> itemConstrainMap = new HashMap<>();
        for (T itemConstraint : findItemConstList) {
            Integer itemTypeId = function.apply(itemConstraint);
            List<T> itemConstrainList = itemConstrainMap.get(itemTypeId);
            if (itemConstrainList == null) {
                itemConstrainList = new LinkedList<>();
                itemConstrainMap.put(itemTypeId, itemConstrainList);
            }
            itemConstrainList.add(itemConstraint);
        }
        return itemConstrainMap;
    }

    /**
     * Vytvoří mapu z listu podle zadaného klíče.
     *
     * @param entities       seznam entit
     * @param getKeyFunction funkce pro získání klíče z entity (typicky id entity)
     * @return mapa
     */
    public static <T> Map<Integer, T> createEntityMap(final Collection<T> entities,
                                                      final Function<T, Integer> getKeyFunction) {
        Map<Integer, T> result = new HashMap<>();
        for (T entity : entities) {

            Integer key = getKeyFunction.apply(entity);
            result.put(key, entity);
        }
        return result;
    }
}
