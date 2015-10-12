package cz.tacr.elza.ui.components.attribute;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.vaadin.data.Property;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import cz.req.ax.AxAction;
import cz.req.ax.AxForm;
import cz.req.ax.AxItemContainer;
import cz.req.ax.Components;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.ui.components.autocomplete.Autocomplete;


/**
 * @author Martin Šlapa
 * @since 2.10.2015
 */
public class AttributValue extends CssLayout implements Components {

    AxForm<ArrDescItem> form;

    private DragAndDropWrapper wrapper;

    private AttributeValuesLoader attributeValuesLoader;

    private ComboBox specificationCombo;

    private Integer bckDescItemObjectId;

    public AttributValue(ArrDescItem descItemExt, List<RulDescItemSpec> descItemSpecs, RulDataType dataType,
                         AxAction deleteAction, DragAndDropWrapper wrapper, final AttributeValuesLoader attributeValuesLoader) {
        this.wrapper = wrapper;
        this.attributeValuesLoader = attributeValuesLoader;
        form = AxForm.init(descItemExt).layoutCss();
        form.addStyleName("attribut-value");

        if (descItemSpecs != null && descItemSpecs.size() > 0) {
            AxItemContainer<RulDescItemSpec> container = AxItemContainer.init(RulDescItemSpec.class);
            container.addAll(descItemSpecs);
            specificationCombo = form.addCombo(null, "descItemSpec", container, "name").field();
            specificationCombo.addStyleName("attribut-spec");
        }

        switch (dataType.getCode()) {
            case "INT":
            case "UNITID":
                form.addField(null, "value").style("small-input");
                break;
            case "STRING":
                form.addField(null, "value").style("long-input");
                break;
            case "UNITDATE":
            case "TEXT":
            case "COORDINATES":
                form.addField(null, "value", TextArea.class).field().setNullRepresentation("");
                break;
            case "FORMATTED_TEXT":
                form.addRichtext(null, "value").field().setNullRepresentation("");
                break;
            case "PARTY_REF":
                Autocomplete partyRefAutoc = createPartyRefAutocomplete();
                form.getFieldGroup().bind(partyRefAutoc, "party");
                form.addComponent(partyRefAutoc);
                break;
            case "RECORD_REF":
                final Autocomplete recordRefAutoc = createRecordRefAutocomplete();
                form.getFieldGroup().bind(recordRefAutoc, "record");
                form.addComponent(recordRefAutoc);

                specificationCombo.setNullSelectionAllowed(false);
                specificationCombo.addValueChangeListener((value) -> {
                    recordRefAutoc.setValue(null);
                    recordRefAutoc.reloadItems("");
                });

                break;
            case "DECIMAL":
                AxForm.AxField field = form.addField(null, "value", TextField.class);
                ((TextField) field.field()).setConverter(new StringToBigDecimalConverter());
                field.style("long-input");
                break;
            default:
                throw new IllegalStateException("Typ '" + dataType.getCode() + "' není implementován");
        }

        addComponent(wrapper);
        addComponent(form);
        addComponent(deleteAction.button());
    }

    private Autocomplete createPartyRefAutocomplete(){
        Autocomplete autocomplete = new Autocomplete((text)->{
            return attributeValuesLoader.loadPartyRefItemsFulltext(text);
        });
        initAutocomplete(autocomplete);

        return autocomplete;
    }

    private Autocomplete createRecordRefAutocomplete(){
        Autocomplete autocomplete = new Autocomplete((text)->{
            if(specificationCombo == null){
                throw new IllegalStateException("Musí být umožněn výběr typu stecifikace.");
            }

            RulDescItemSpec selectedSpec = (RulDescItemSpec) specificationCombo.getValue();
            return attributeValuesLoader.loadRecordRefItemsFulltext(text, selectedSpec);
        });
        initAutocomplete(autocomplete);

        return autocomplete;
    }

    private void initAutocomplete(final Autocomplete autocomplete) {
        autocomplete.reloadItems("");
        autocomplete.addTextChangeListener((event) -> {
            if (StringUtils.isBlank(event.getText())) {
                autocomplete.reloadItems("");
            }
        });
    }


    public DragAndDropWrapper getWrapper() {
        return wrapper;
    }

    public ArrDescItem commit() {
        ArrDescItem descItem = form.commit();
        bckDescItemObjectId = descItem.getDescItemObjectId();
        return descItem;
    }

    public void revert() {
        ArrDescItem descItem = form.getValue();
        descItem.setDescItemObjectId(bckDescItemObjectId);
    }

    private class SpecificationValueChangeListener implements Property.ValueChangeListener{

        @Override
        public void valueChange(final Property.ValueChangeEvent event) {

        }
    }

}
