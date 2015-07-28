package cz.tacr.elza.ax;

import com.vaadin.data.util.BeanItem;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import cz.tacr.elza.ax.*;


public class AxBeanTable<T extends IdObject<Integer>> extends AxTable<T> {

    cz.tacr.elza.ax.AxBeanContainer<T> container;

    public static <T extends IdObject<Integer>> AxBeanTable<T> init(cz.tacr.elza.ax.AxBeanContainer<T> type) {
        return new AxBeanTable<>(type);
    }

    public AxBeanTable(final cz.tacr.elza.ax.AxBeanContainer<T> container) {
        this.container = container;
        getTable().addValueChangeListener(event -> {
            if (selectListener == null) return;
            if (event == null || event.getProperty() == null || event.getProperty().getValue() == null) {
                selectListener.beanEvent(null);
            } else {
                BeanItem<T> item = container.getItem(event.getProperty().getValue());
                selectListener.beanEvent(item == null ? null : item.getBean());
            }
        });
    }

    @Override
    public cz.tacr.elza.ax.AxBeanContainer<T> getContainer() {
        return container;
    }

    public AxBeanTable<T> supplier(Supplier<List<T>> supplier) {
        if (container instanceof AxContainer) {
            ((AxContainer) container).supplier(supplier);
        }
        return this;
    }

    public AxBeanTable<T> supplier(Function<? extends AxRepository<T>, List<T>> supplier) {
        if (container instanceof AxContainer) {
            ((AxContainer) container).supplier(supplier);
        }
        return this;
    }

    public void refresh() {
        //TODO Advocate?
        container.removeAllContainerFilters();
        if (containerFilter != null) {
            container.addContainerFilter(containerFilter);
        }
        if (container instanceof AxContainer) {
            ((AxContainer) container).refresh();
        }
        super.refresh();
    }
}
