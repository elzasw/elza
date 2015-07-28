package cz.tacr.elza.ax;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * V podstatě programová emulace Javovského switche.
 *
 * @param <SOURCE> vstupní typ switche
 * @param <TARGET> výstupní typ switche
 * @author <a href="mailto:jan.pikl@marbes.cz">Jan Pikl</a>
 *         Date: 14.4.2015
 */
class Switch<SOURCE, TARGET> {

    private Map<SOURCE, Supplier<TARGET>> suppliers = new HashMap<>();
    private Consumer<TARGET> consumer;

    public Switch<SOURCE, TARGET> on(SOURCE value, TARGET target) {
        return on(value, () -> target);
    }

    public Switch<SOURCE, TARGET> on(SOURCE value, Supplier<TARGET> supplier) {
        suppliers.put(value, supplier);
        return this;
    }

    public Switch<SOURCE, TARGET> action(Consumer<TARGET> consumer) {
        this.consumer = consumer;
        return this;
    }

    public Switch<SOURCE, TARGET> set(SOURCE value) {
        if (consumer != null) {
            consumer.accept(get(value));
        }
        return this;
    }

    public TARGET get(SOURCE value) {
        Supplier<TARGET> supplier = suppliers.get(value);
        return supplier != null ? supplier.get() : null;
    }

}
