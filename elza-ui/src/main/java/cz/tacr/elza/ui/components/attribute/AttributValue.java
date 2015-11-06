package cz.tacr.elza.ui.components.attribute;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.vaadin.data.Property;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import cz.req.ax.AxAction;
import cz.req.ax.AxForm;
import cz.req.ax.AxItemContainer;
import cz.req.ax.Components;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrDescItemUnitdate;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulDescItemSpec;
import cz.tacr.elza.ui.components.autocomplete.Autocomplete;
import cz.tacr.elza.ui.utils.UnitDateConvertor;


/**
 * Komponenta pro hodnotu atributu.
 *
 * @author Martin Šlapa
 * @since 2.10.2015
 */
public class AttributValue extends CssLayout implements Components {

    /**
     * Formulář hodnoty atributu
     */
    AxForm<ArrDescItem> form;

    /**
     * Wrapper pro jednoduchou změnu pozice
     */
    private DragAndDropWrapper wrapper;

    /**
     * Loader hodnot atributu
     */
    private AttributeValuesLoader attributeValuesLoader;

    /**
     * Kombobox pro specifikaci atributu
     */
    private ComboBox specificationCombo;

    /**
     * Pomocná proměnná pro zálohování identifikátor hodnoty atributu.
     */
    private Integer bckDescItemObjectId;

    /**
     * Konstruktor pro novou hodnotu atributu
     * @param descItemExt   hodnota atributu
     * @param descItemSpecs specifikace atributu
     * @param dataType      typ atributu
     * @param deleteAction  akce při smazání
     * @param wrapper       obalení pro pozicování
     * @param attributeValuesLoader loader hodnot atributu
     * @param calendarTypes typy kalendářů
     */
    public AttributValue(ArrDescItem descItemExt, List<RulDescItemSpec> descItemSpecs, RulDataType dataType,
                         AxAction deleteAction, DragAndDropWrapper wrapper, final AttributeValuesLoader attributeValuesLoader, List<ArrCalendarType> calendarTypes) {
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

        AxItemContainer<ArrCalendarType> calendarTypesContainer = AxItemContainer.init(ArrCalendarType.class);
        calendarTypesContainer.addAll(calendarTypes);

        switch (dataType.getCode()) {
            case "INT":
            case "UNITID":
                form.addField(null, "value").style("small-input");
                break;
            case "STRING":
                form.addField(null, "value").style("long-input");
                break;
            case "UNITDATE":
                ArrDescItemUnitdate descItemUnitDate = (ArrDescItemUnitdate) descItemExt;
                if (descItemUnitDate.getFormat() != null) {
                    descItemUnitDate.setFormat(UnitDateConvertor.convertToString(descItemUnitDate));
                }
                form.addStyleName("datace");
                form.addCombo("Typ kalendare", "calendarType", calendarTypesContainer, "name").required();
                form.addField("Hodnota", "format");
                break;
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
            case "PACKET_REF":
                Autocomplete paketRefAutoc = createPacketRefAutocomplete();
                form.getFieldGroup().bind(paketRefAutoc, "packet");
                form.addComponent(paketRefAutoc);
                  break;
            default:
                throw new IllegalStateException("Typ '" + dataType.getCode() + "' není implementován");
        }

        addComponent(wrapper);
        addComponent(form);
        addComponent(deleteAction.button());
    }

    /**
     * Vytvoření autocomplete objektu pro partyRef
     * @return  autocomplete objekt
     */
    private Autocomplete createPartyRefAutocomplete(){
        Autocomplete autocomplete = new Autocomplete((text)->{
            return attributeValuesLoader.loadPartyRefItemsFulltext(text);
        });
        initAutocomplete(autocomplete);

        return autocomplete;
    }

    /**
     * Vytvoření autocomplete objektu pro packetRef
     * @return autocomplete objekt
     */
    private Autocomplete createPacketRefAutocomplete(){
        Autocomplete autocomplete = new Autocomplete((text)->{
            return attributeValuesLoader.loadPacketRefItemsFulltext(text);
        });
        initAutocomplete(autocomplete);

        return autocomplete;
    }

    /**
     * Vytvoření autocomplete objektu pro recordRef
     * @return autocomplete objekt
     */
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

    /**
     * Inicializace autocomplete objektu
     * @param autocomplete inicializovaný autocomplete objekt
     */
    private void initAutocomplete(final Autocomplete autocomplete) {
        autocomplete.reloadItems("");
        autocomplete.addTextChangeListener((event) -> {
            if (StringUtils.isBlank(event.getText())) {
                autocomplete.reloadItems("");
            }
        });
    }

    /**
     * Navrací wrapper
     * @return  wrapper
     */
    public DragAndDropWrapper getWrapper() {
        return wrapper;
    }

    /**
     * Provede validaci dat a navrací výslednou hodnotu atributu
     * @return  hodnota atributu
     */
    public ArrDescItem commit() {
        ArrDescItem descItem = form.commit();
        if (descItem instanceof ArrDescItemUnitdate) {
            ArrDescItemUnitdate descItemUnitdate = (ArrDescItemUnitdate) descItem;
            UnitDateConvertor.convertToUnitDate(descItemUnitdate.getFormat(), descItemUnitdate);
        }
        bckDescItemObjectId = descItem.getDescItemObjectId();
        return descItem;
    }

    /**
     * Revertování hodnoty atributu
     */
    public void revert() {
        ArrDescItem descItem = form.getValue();
        descItem.setDescItemObjectId(bckDescItemObjectId);
    }

    /**
     * Listener pro změnu hodnoty
     */
    private class SpecificationValueChangeListener implements Property.ValueChangeListener{

        @Override
        public void valueChange(final Property.ValueChangeEvent event) {

        }
    }

}
