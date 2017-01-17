package cz.tacr.elza;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.proxy.HibernateProxy;

import javax.annotation.Nullable;
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


    /**
     * Pokud se nerovanjí objekty, vyhodí výjimku.
     * @param valueA objekt A
     * @param valueB objekt B
     * @param message zpráva výjimky
     */
    public static void checkEquals(final Object valueA, final Object valueB, final String message)
            throws IllegalArgumentException {
        if (!ObjectUtils.equals(valueA, valueB)) {
            throw new IllegalArgumentException(message);
        }
    }


    /**
     * Pokud je entity z DB typu {@link HibernateProxy}, je převedena na DO objekt.
     *
     * @param entity      entita
     * @param tergetClass cílová třída entity (DO objekt class)
     * @param <T>         DO objekt class
     * @return implementace entity
     */
    public static <T> T unproxyEntity(@Nullable final Object entity, Class<T> tergetClass) {
        if (entity == null) {
            return null;
        }

        if (entity instanceof HibernateProxy) {
            return (T) ((HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
        } else {
            return (T) entity;
        }
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
