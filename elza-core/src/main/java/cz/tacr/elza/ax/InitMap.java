package cz.tacr.elza.ax;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 13.1.15
 */
public class InitMap {

    Map<String, Object> map = new HashMap<>();

    public <T> T get(String key, Supplier<T> sup) {
        if (map.containsKey(key)) {
            try {
                return (T) map.get(key);
            } catch (ClassCastException e) {
                //Chybna instance, inicializujeme znovu
            }
        }
        if (sup == null) return null;
        T var = sup.get();
        map.put(key, var);
        return var;
    }

    public <T> T set(String key, T var) {
        map.put(key, var);
        return var;
    }
}
