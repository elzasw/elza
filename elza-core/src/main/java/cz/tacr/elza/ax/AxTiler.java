package cz.tacr.elza.ax;

import com.vaadin.data.util.AbstractBeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import org.springframework.util.Assert;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import cz.tacr.elza.ax.*;


public class AxTiler<T> extends CssLayout {

    //    String entityName;
    String propertyId;
    AbstractBeanContainer<?, T> container;
    Consumer<T> action;
    Function<BeanItem<T>, AbstractComponent> factory;
    BiFunction<BeanItem<T>, AbstractComponent, AbstractComponent> modifier;

    public AxTiler() {
        addStyleName("tiler");
        setSizeFull();
        factory = (bean) -> {
            Assert.notNull(bean);
            Object caption = bean.getItemProperty(propertyId).getValue();
            AbstractComponent button = new Button(caption.toString(), event -> {
                if (action == null) return;
                T data = (T) event.getButton().getData();
                action.accept(data);
            });
            if (modifier != null) {
                button = modifier.apply(bean, button);
            }
            button.addStyleName("tile");
            button.setData(bean.getBean());
            return button;
        };
    }

    public AxTiler<T> container(AbstractBeanContainer<?, T> container) {
        Assert.notNull(propertyId, "Set propertyId before container!");
        this.container = container;
        refresh();
        return this;
//        entityName = container.getRepository().entityClass().getSimpleName();
    }

    public AxTiler<T> action(Consumer<T> action) {
        this.action = action;
        return this;
    }

    public AxTiler<T> factory(Function<BeanItem<T>, AbstractComponent> factory) {
        this.factory = factory;
        return this;
    }

    public AxTiler<T> modifier(BiFunction<BeanItem<T>, AbstractComponent, AbstractComponent> modifier) {
        this.modifier = modifier;
        return this;
    }

    public AxTiler<T> style(String style) {
        addStyleName(style);
        return this;
    }

    public AxTiler<T> propertyId(String propertyId) {
        this.propertyId = propertyId;
        return this;
    }

    public AbstractBeanContainer<?, T> getContainer() {
        return container;
    }

    public void refresh() {
        removeAllComponents();
        if (container == null) return;
        if (container instanceof cz.tacr.elza.ax.AxContainer) {
            ((cz.tacr.elza.ax.AxContainer) container).refresh();
        }
        for (Object id : container.getItemIds()) {
            BeanItem<T> bean = container.getItem(id);
            addComponent(factory.apply(bean));
        }
    }

}
