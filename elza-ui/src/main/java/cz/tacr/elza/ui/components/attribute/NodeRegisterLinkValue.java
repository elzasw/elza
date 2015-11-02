package cz.tacr.elza.ui.components.attribute;

import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import cz.req.ax.AxAction;
import cz.req.ax.AxForm;
import cz.req.ax.AxItemContainer;
import cz.req.ax.Components;
import cz.tacr.elza.controller.RegistryManager;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.ui.components.autocomplete.Autocomplete;
import cz.tacr.elza.ui.components.autocomplete.AutocompleteItem;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Jedna hodnota přiřazení nodu a hesla s komponentou jejího výběru.
 */
public class NodeRegisterLinkValue extends CssLayout implements Components {

    AxForm<ArrNodeRegister> form;

    private ComboBox specificationCombo;

    private RegistryManager registryManager;

    public NodeRegisterLinkValue(final ArrNodeRegister nodeRegister, final List<RegRegisterType> registerTypes,
                                 final AxAction deleteAction, final RegistryManager registryManager) {

        this.registryManager = registryManager;

        form = AxForm.init(nodeRegister).layoutCss();
        form.addStyleName("attribut-value");

        if (registerTypes != null && registerTypes.size() > 0) {
            AxItemContainer<RegRegisterType> registerTypesContainer = AxItemContainer.init(RegRegisterType.class);
            registerTypesContainer.addAll(registerTypes);
            specificationCombo = new ComboBox("", registerTypesContainer);
            specificationCombo.setItemCaptionPropertyId("name");
            specificationCombo.addStyleName("attribut-spec");

            // předvýběr typou u existující položky
            RegRecord record = registryManager.getRecord(nodeRegister.getRecord().getRecordId());
            BeanItem<RegRegisterType> typeItem = registerTypesContainer.getItem(record.getRegisterType());
            specificationCombo.setValue(typeItem.getBean());

            form.addComponent(specificationCombo);
        }

        final Autocomplete recordRefAutoc = createRecordRefAutocomplete();
        form.getFieldGroup().bind(recordRefAutoc, "record");
        form.addComponent(recordRefAutoc);

        specificationCombo.setNullSelectionAllowed(false);
        specificationCombo.addValueChangeListener((value) -> {
            recordRefAutoc.setValue(null);
            recordRefAutoc.reloadItems("");
        });

        addComponent(form);
        addComponent(deleteAction.button());
    }

    private Autocomplete createRecordRefAutocomplete() {
        Autocomplete autocomplete = new Autocomplete((text) -> {
            if (specificationCombo == null) {
                throw new IllegalStateException("Musí existovat typ rejstříku.");
            }

            RegRegisterType registerType = (RegRegisterType) specificationCombo.getValue();
            return loadRegRecords(text, registerType);
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

    private List<AutocompleteItem> loadRegRecords(final String text, final RegRegisterType registerType) {

        List<RegRecord> recordList = registryManager.findRecord(text, 0, 50, new Integer[] {registerType.getRegisterTypeId()});

        List<AutocompleteItem> result = new ArrayList<>(recordList.size());
        for (final RegRecord regRecord : recordList) {
            result.add(new AutocompleteItem(regRecord, regRecord.getRecord()));
        }

        return result;
    }

    public ArrNodeRegister commit() {
        ArrNodeRegister nodeRegister = form.commit();
        return nodeRegister;
    }

}
