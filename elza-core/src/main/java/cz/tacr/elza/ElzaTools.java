package cz.tacr.elza;

import org.apache.commons.collections4.CollectionUtils;

import java.time.LocalDateTime;
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
        if(CollectionUtils.isEmpty(entities)){
            return result;
        }

        for (T entity : entities) {

            Integer key = getKeyFunction.apply(entity);
            result.put(key, entity);
        }
        return result;
    }

    /**
     * Vytvoří nový filtrovaný seznam podle filtru.
     *
     * @param entities       seznam hodnot, které budeme filtrovat
     * @param filterFunction funkce pro filtrování
     * @param <T>            typ entity
     * @return vyfiltrovaný seznam
     */
    public static <T> List<T> filter(final Collection<T> entities, final Function<T, Boolean> filterFunction) {

        List<T> result = new LinkedList<>();
        if (entities == null) {
            return result;
        }
        for (T entity : entities) {
            if (filterFunction.apply(entity)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * Aktuální datum v neformátováne podobě. Hodí se pro doplnění unique fieldů v testech.
     * @return  řetězec ve formátu např. "2016-01-05T22:05:47.859"
     */
    public static String getStringOfActualDate() {
        return LocalDateTime.now().toString();
    }

}
