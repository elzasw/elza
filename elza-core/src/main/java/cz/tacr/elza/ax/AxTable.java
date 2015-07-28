package cz.tacr.elza.ax;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import cz.tacr.elza.ax.BeanEventListener;
import cz.tacr.elza.ax.BeanFilter;
import cz.tacr.elza.ax.ComponentWrapper;
import cz.tacr.elza.ax.Refresh;


public abstract class AxTable<T> implements ComponentWrapper, Refresh {

    List<String> visibleColumns = new ArrayList<>();
    Container.Filter containerFilter;
    BeanEventListener<T> selectListener;
    BeanEventListener<T> selectPredicate;
    Refresh refreshListener;
    Table table;

    public abstract Container getContainer();

    @Override
    public Component getComponent() {
        return getTable();
    }

    public Table getTable() {
        if (table == null) {
            table = new Table(null, getContainer()) {
                @Override
                public void setPageLength(int pageLength) {
                    super.setPageLength(pageLength);
                    // Pokud chceme zobrazovat jen cast (nastaveno pageLength) tak scroll musime povolit
                    if (pageLength > 0) {
                        table.addStyleName("hack-allow-v-scroll");
                    } else {
                        table.removeStyleName("hack-allow-v-scroll");
                    }
                }
            };
            table.addStyleName("hack-noscroll");
            table.setWidth(100, Sizeable.Unit.PERCENTAGE);
//            table.setWidth(100, Sizeable.Unit.PERCENTAGE);
            table.setPageLength(0);
            table.setSelectable(true);
            table.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        }
        return table;
    }

    public T getValue() {
        try {
            Object value = table.getValue();
            if (value == null) return null;
            if (value instanceof Integer) {
                Item item = getContainer().getItem(value);
                if (item instanceof BeanItem) {
                    return (T) ((BeanItem) item).getBean();
                } else {
                    throw new UnsupportedOperationException();
                }
            } else {
                return (T) value;
            }
        } catch (Exception ex) {
            return null;
        }
    }

    public void setValue(T value) {
        table.setValue(value);
    }

    public AxTable<T> style(String... styleClasses) {
        for (String styleClass : styleClasses) {
            getTable().addStyleName(styleClass);
        }
        return this;
    }

    //TODO Refreshable FunctionInterface???
    public AxTable<T> refresh(Refresh listener) {
        refreshListener = listener;
        return this;
    }

    public AxTable<T> select(BeanEventListener<T> listener) {
        selectListener = listener;
        return this;
    }

    public AxTable<T> filter(final Predicate<T> filter) {
        return filter(new BeanFilter<T>() {

            @Override
            public boolean passFilter(T bean) {
                return filter.test(bean);
            }
        });
    }

    public AxTable<T> filter(Container.Filter filter) {
        this.containerFilter = filter;
        return this;
    }

    public AxTable<T> columns(String... propertyIds) {
        if (propertyIds == null) {
            visibleColumns.clear();
            table.setVisibleColumns(new Object[0]);
        } else {
            for (String propertyId : propertyIds) {
                visibleColumns.add(propertyId);
            }
            table.setVisibleColumns((Object[]) propertyIds);
//            table.setColumnExpandRatio(propertyIds[0], 1f);
        }
        return this;
    }

    public ColumnFactory<T> column(String propertyId) {
        return new ColumnFactory<T>(this, propertyId);
    }

    public ColumnFactory<T> column(String propertyId, Table.ColumnGenerator columnGenerator) {
        table.addGeneratedColumn(propertyId, columnGenerator);
        return column(propertyId);
    }

    public AxTable<T> done() {
        table.setVisibleColumns(visibleColumns.toArray());
        refresh();
        return this;
    }

    public static class ColumnFactory<T> {

        private AxTable<T> table;
        private String property;

        public ColumnFactory(AxTable<T> table, String property) {
            this.table = table;
            this.table.visibleColumns.add(property);
            this.property = property;
            //TODO Check nested??
            //this.table.getContainer().pro
        }

        public ColumnFactory<T> width(int pixels) {
            table.getTable().setColumnWidth(property, pixels);
            return this;
        }

        public ColumnFactory<T> expand(float ratio) {
            table.getTable().setColumnExpandRatio(property, ratio);
            return this;
        }

        public ColumnFactory<T> converter(Converter<String, ?> converter) {
            table.getTable().setConverter(property, converter);
            return this;
        }

        public ColumnFactory<T> alignRight() {
            table.getTable().setColumnAlignment(property, Table.Align.RIGHT);
            return this;
        }

        public ColumnFactory<T> column(String propertyId) {
            return new ColumnFactory(table, propertyId);
        }

        public ColumnFactory<T> column(String propertyId, Table.ColumnGenerator columnGenerator) {
            table.getTable().addGeneratedColumn(propertyId, columnGenerator);
            return column(propertyId);
        }

        public AxTable<T> done() {
            return table.done();
        }
    }

    public void refresh() {
        table.refreshRowCache();
        if (refreshListener != null)
            refreshListener.refresh();
    }
}
