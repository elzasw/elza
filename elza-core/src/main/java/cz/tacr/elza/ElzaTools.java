/**
 * 
 */
package cz.tacr.elza;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cz.tacr.elza.api.RulArrangementType;

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
     * @param f - funkce vracejici hodnotu klíčš pro grupovani.
     * @return výsledná mapa.
     */
    public static <T> Map<Integer, List<T>> createGroupMap(final List<T> findItemConstList,
            final Function<T, Integer> f) {
        Map<Integer, List<T>> itemConstrainMap = new HashMap<>();
        for (T itemConstraint : findItemConstList) {
            Integer itemTypeId = f.apply(itemConstraint);
            List<T> itemConstrainList = itemConstrainMap.get(itemTypeId);
            if (itemConstrainList == null) {
                itemConstrainList = new LinkedList<>();
                itemConstrainMap.put(itemTypeId, itemConstrainList);
            }
            itemConstrainList.add(itemConstraint);
        }
        return itemConstrainMap;
    }
}
