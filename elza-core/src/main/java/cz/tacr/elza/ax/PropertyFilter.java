package cz.tacr.elza.ax;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 15.12.14
 */
public class PropertyFilter implements Container.Filter {

    private String propertyName;
    private Object propertyValue;

    public PropertyFilter(String propertyName, Object propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    @Override
    public boolean passesFilter(Object o, Item item) throws UnsupportedOperationException {
        Property property = item.getItemProperty(propertyName);
        if (property == null) {
            throw new IllegalArgumentException("Property " + propertyName + " not found");
        }
        if (propertyValue == null) {
            return property.getValue() == null;
        }
        return propertyValue.equals(property.getValue());
    }

    @Override
    public boolean appliesToProperty(Object o) {
        return true;
    }
}
