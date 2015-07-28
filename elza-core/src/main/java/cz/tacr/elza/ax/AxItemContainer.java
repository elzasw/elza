package cz.tacr.elza.ax;

import com.vaadin.data.util.BeanItemContainer;

import java.util.List;

public class AxItemContainer<T> extends BeanItemContainer<T> {

    public static <T> AxItemContainer<T> init(Class<T> type) {
        return new AxItemContainer<>(type);
    }

    protected AxItemContainer(Class<T> type) {
        super(type);
    }

    public void replaceAllItems(List<T> items) {
        removeAllItems();
        if (items != null) addAll(items);
    }
}
