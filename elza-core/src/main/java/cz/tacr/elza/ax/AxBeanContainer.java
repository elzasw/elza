package cz.tacr.elza.ax;

import java.io.Serializable;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;

import cz.tacr.elza.ax.IdObject;


public class AxBeanContainer<T extends IdObject<Integer>> extends BeanContainer<Integer, T> {

    Class<T> type;

    public static <T extends IdObject<Integer>> AxBeanContainer<T> init(Class<T> type) {
        return new AxBeanContainer<>(type);
    }

    protected AxBeanContainer(Class<T> type) {
        super(type);
        this.type = type;
    }

    public Class<T> getType() {
        return type;
    }

    public BeanItem<T> newItem() {
        return new BeanItem<T>(newBean());
    }

    public T newBean() {
        try {
            return type.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Chyba newInstance typu "+type,e);
        }
    }

    public Integer delete(Object object) {
        Integer id = getId(object);
        super.removeItem(id);
        return id;
    }

    protected Integer getId(Object object) {
        if (object instanceof Integer) {
            return (Integer) object;
        }
        if (object instanceof IdObject) {
            Serializable id = ((IdObject) object).getId();
            if (id instanceof Integer) {
                return (Integer) id;
            }
        }
        throw new IllegalArgumentException("Objekt nemá ID typu Integer");
    }

}
