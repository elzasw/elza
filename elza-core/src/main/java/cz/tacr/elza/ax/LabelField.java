package cz.tacr.elza.ax;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Label;

public class LabelField extends CustomField<String> {

    private Label label = new Label("...");

    public LabelField() {
        setSizeUndefined();
        addStyleName("label-field");
        label.setSizeUndefined();
    }

    @Override
    protected Component initContent() {
        return label;
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    protected void setInternalValue(String newValue) {
        super.setInternalValue(newValue);
        label.setValue(newValue);
    }

}
