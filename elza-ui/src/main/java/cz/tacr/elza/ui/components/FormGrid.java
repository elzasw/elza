package cz.tacr.elza.ui.components;

import com.vaadin.ui.Component;



/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.9.2015
 */
public class FormGrid extends CssLayout implements Components {


    public FormGrid() {
        setSizeUndefined();
        setStyleName("form-grid");
    }


    public CssLayout addRow(final String caption, final String value) {
        return addRow(newLabel(caption), newLabel(value));
    }

    public CssLayout addRow(final String caption, final Component value) {
        return addRow(newLabel(caption), value);
    }

    public CssLayout addRow(final Component caption, final Component value) {
        CssLayout row = cssLayoutExt("grid-row");
        row.addComponent(caption);
        row.addComponent(value);
        addComponent(row);
        return this;
    }


    public FormGrid style(final String style){
        addStyleName(style);
        return this;
    }

    public FormGrid setRowSpacing(final boolean spacing) {
        if (spacing) {
            addStyleName("row-spacing");
        } else {
            removeStyleName("row-spacing");
        }

        return this;
    }



}
