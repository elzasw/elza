package cz.tacr.elza;

import java.util.*;
import java.util.function.Function;

import cz.tacr.elza.domain.ArrAsyncRequest;
import cz.tacr.elza.domain.ArrRequest;
import org.apache.commons.collections4.CollectionUtils;


/**
 * Obecné pomocné metody.
 *
 * @author vavrejn
 *
 */
public class ElzaTools {

    private ElzaTools() {
        // Cannot be instantiated
    }

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
     * vytvoří z listu mapu front zagrupovanou podle zadaného klíče.
     *
     * @param <T> typ zpracovávaného objektu.
     * @param findItemConstList list pro grupovani
     * @param function - funkce vracejici hodnotu klíčš pro grupovani.
     * @return výsledná mapa.
     */
    public static <T> Map<Integer, Queue<T>> createGroupQueueMap(final List<T> findItemConstList,
                                                            final Function<T, Integer> function) {
        Map<Integer, Queue<T>> itemConstrainMap = new HashMap<>();
        for (T itemConstraint : findItemConstList) {
            Integer itemTypeId = function.apply(itemConstraint);
            Queue<T> itemConstrainList = itemConstrainMap.get(itemTypeId);
            if (itemConstrainList == null) {
                itemConstrainList = new LinkedList<>();
                itemConstrainMap.put(itemTypeId, itemConstrainList);
            }
            itemConstrainList.add(itemConstraint);
        }
        return itemConstrainMap;
    }

    /**
     * vytvoří z listu mapu front zagrupovanou podle zadaného klíče.
     *
     * @param <T> typ zpracovávaného objektu.
     * @param findItemConstList list pro grupovani
     * @param function - funkce vracejici hodnotu klíčš pro grupovani.
     * @return výsledná mapa.
     */
    public static <T> Map<Integer, PriorityQueue<ArrAsyncRequest>> createGroupPriorityQueueMap(final List<ArrAsyncRequest> findItemConstList,
                                                                 final Function<ArrAsyncRequest, Integer> function) {
        Map<Integer, PriorityQueue<ArrAsyncRequest>> itemConstrainMap = new HashMap<>();
        for (ArrAsyncRequest itemConstraint : findItemConstList) {
            Integer itemTypeId = function.apply(itemConstraint);
            PriorityQueue<ArrAsyncRequest> itemConstrainList = itemConstrainMap.get(itemTypeId);
            if (itemConstrainList == null) {
                itemConstrainList = new PriorityQueue<ArrAsyncRequest>(1000,new Comparator<ArrAsyncRequest>() {
                    @Override
                    public int compare(ArrAsyncRequest r1, ArrAsyncRequest r2) {
                        if(r1.getPriority() == r2.getPriority()) {
                            if(r1.getAsyncRequestId() > r2.getAsyncRequestId())
                                return 1;
                            else if(r1.getAsyncRequestId() < r2.getAsyncRequestId())
                                return -1;
                            return 0;
                        } else {
                            return (r2.getPriority() - r1.getPriority());
                        }

                    }
                });
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
     * Přidá parametry do url.
     *
     * @param url    vstupní urů
     * @param params parametry, které se můžou použít
     * @return výsůedná url
     */
    public static String bindingUrlParams(final String url, UrlParams params) {
        if (params == null || url == null) {
            return url;
        }
        String result = url;

        for (Map.Entry<String, String> entry : params.getProperties().entrySet()) {
            result = result.replaceAll("\\{" + entry.getKey() + "\\}", entry.getValue());
        }
        return result;
    }

    /**
     * Vytvoření parametru pro url.
     *
     * @return parametry
     */
    public static UrlParams createUrlParams() {
        return new UrlParams();
    }

    /**
     * Struktura pro uchování parametrů pro url.
     */
    public static class UrlParams {
        Map<String, String> properties = new HashMap<>();

        public UrlParams add(final String key, final Object value) {
            if (value != null) {
                properties.put(key, value.toString());
            }
            return this;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }


}
