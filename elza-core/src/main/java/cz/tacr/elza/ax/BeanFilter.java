package cz.tacr.elza.ax;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;

public abstract class BeanFilter<T> implements Container.Filter {

    public abstract boolean passFilter(T bean);

    @Override
    public boolean passesFilter(Object itemId, Item item) throws UnsupportedOperationException {
        Object bean = item;
        if (item instanceof BeanItem) {
            bean = ((BeanItem) item).getBean();
        }
        try {
            return passFilter((T) bean);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean appliesToProperty(Object propertyId) {
        return true;
    }

}
