package cz.tacr.elza.ui.components;

import java.util.List;

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.TextArea;

import cz.req.ax.AxAction;
import cz.req.ax.AxForm;
import cz.req.ax.AxItemContainer;
import cz.req.ax.Components;
import cz.tacr.elza.domain.ArrDescItemExt;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;


/**
 * @author Martin Šlapa
 * @since 2.10.2015
 */
public class AttributValue extends CssLayout implements Components {

    AxForm<ArrDescItemExt> form;

    private DragAndDropWrapper wrapper;

    public AttributValue(ArrDescItemExt a, List<RulDescItemSpec> descItemSpecs, RulDataType dataType, AxAction deleteAction, DragAndDropWrapper wrapper) {
        this.wrapper = wrapper;
        form = AxForm.init(a).layoutCss();
        form.addStyleName("attribut-value");

        if (descItemSpecs != null && descItemSpecs.size() > 0) {
            AxItemContainer<RulDescItemSpec> container = AxItemContainer.init(RulDescItemSpec.class);
            container.addAll(descItemSpecs);
            form.addCombo(null, "descItemSpec", container, "name").field().addStyleName("attribut-spec");
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
                form.addField(null, "data", TextArea.class).field().setNullRepresentation("");
                break;
            case "FORMATTED_TEXT":
                form.addRichtext(null, "data").field().setNullRepresentation("");
                break;
            default:
                throw new IllegalStateException("Typ '" + dataType.getCode() + "' není implementován");
        }

        addComponent(wrapper);
        addComponent(form);
        addComponent(deleteAction.button());
    }

    public DragAndDropWrapper getWrapper() {
        return wrapper;
    }

    public ArrDescItemExt commit() {
        return form.commit();
    }

}
