package cz.tacr.elza.ui.components;

import com.vaadin.ui.DragAndDropWrapper;

import cz.tacr.elza.domain.ArrDescItemExt;

/**
 * @author Martin Šlapa
 * @since 2.10.2015
 */
public class AttributDragAndDropWrapper extends DragAndDropWrapper {

    private AttributValue attributValue;

    public AttributDragAndDropWrapper(AttributValue attributValue) {
        super(attributValue);
        addStyleName("wrapper-item");
        setSizeUndefined();
        this.attributValue = attributValue;
    }

    public ArrDescItemExt commit() {
        return attributValue.commit();
    }
}
