package cz.tacr.elza.ui.components;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.TextArea;

import cz.req.ax.AxAction;
import cz.req.ax.AxForm;
import cz.req.ax.AxItemContainer;
import cz.req.ax.Components;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;


public class AttributValue extends CssLayout implements Components {

    AxForm<ArrDescItemExt> form;

    public AttributValue(ArrDescItemExt a, List<RulDescItemSpec> descItemSpecs, RulDataType dataType, AxAction deleteAction) {
        form = AxForm.init(a).layoutCss();
        form.addStyleName("attribut-value");

        if (descItemSpecs != null && descItemSpecs.size() > 0) {
            AxItemContainer<RulDescItemSpec> container = AxItemContainer.init(RulDescItemSpec.class);
            container.addAll(descItemSpecs);
            form.addCombo(null, "descItemSpec", container, "name");
        }

        switch (dataType.getCode()) {
            case "INT":
            case "UNITID":
                form.addField(null, "data").style("small-input");
                break;
            case "STRING":
                form.addField(null, "data").style("long-input");
                break;
            case "UNITDATE":
            case "TEXT":
            case "COORDINATES":
                form.addField(null, "data", TextArea.class);
                break;
            case "FORMATTED_TEXT":
                form.addRichtext(null, "data");
                break;
            default:
                throw new IllegalStateException("Typ '" + dataType.getCode() + "' není implementován");
        }

        addComponent(form);
        addComponent(deleteAction.button());
    }

    public ArrDescItemExt commit() {
        return form.commit();
    }

    /*@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        TODO: dopsat správně porovnání pro ChildComponentContainer

        if (o == null) {
            return false;
        }

        return true;
    }*/

}
