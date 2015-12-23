package cz.tacr.elza.ui.window;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.vaadin.data.Validator;
import com.vaadin.ui.Label;
import com.vaadin.ui.Upload;

import cz.req.ax.AxAction;
import cz.req.ax.AxComboBox;
import cz.req.ax.AxContainer;
import cz.req.ax.AxForm;
import cz.req.ax.AxWindow;
import cz.tacr.elza.api.vo.ImportDataFormat;
import cz.tacr.elza.api.vo.XmlImportConfig;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.controller.XmlImportManager;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.ui.components.Components;
import cz.tacr.elza.ui.utils.ElzaNotifications;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 22.12.2015
 */
public class XmlImportWindow extends AxWindow implements Upload.SucceededListener,
                                                         Upload.FailedListener, Upload.Receiver, Components {

    private RuleManager ruleSetManager;
    private XmlImportManager xmlImportManager;

    private AxContainer<RulArrangementType> arTypeContainer;
       private Label lblUpload;
    private AxContainer<RulRuleSet> ruleSetContainer;

    private XmlImportConfig config;
    private File file;
    private Upload upload;

    private Runnable refresher;


    public XmlImportWindow(final RuleManager ruleSetManager,
                           final XmlImportManager xmlImportManager,
                           final Runnable refresh) {
        this.ruleSetManager = ruleSetManager;
        this.xmlImportManager = xmlImportManager;
        this.refresher = refresh;

        caption("Xml import");

        final AxForm<XmlImportConfig> form = formularNewFA();

        components(form);
        buttonClose();
        setHeight(480, Unit.PIXELS);

        buttonPrimary(new AxAction<XmlImportConfig>().caption("Importovat").value(form::commit).action(this::saveConfig)
                .exception(ex -> {
                    ex.printStackTrace();
                    ElzaNotifications.showError(ex.getMessage());
                }));

        show();
    }

    private void saveConfig(final XmlImportConfig config) {
        try {
            xmlImportManager.importData(config);
            refresher.run();
            ElzaNotifications.show("Import proběhl úspěšně.");
        } catch (Exception e) {
            e.printStackTrace();
            ElzaNotifications.showError(e.getMessage());
        }
    }

    @Override
    public void uploadFailed(final Upload.FailedEvent failedEvent) {
        ElzaNotifications.showError(failedEvent.getReason().getMessage());

    }

    @Override
    public OutputStream receiveUpload(final String s, final String s1) {
        FileOutputStream fos;
        lblUpload.setValue(s);
        try {
            file = File.createTempFile("xmlImport", s);
            file.deleteOnExit();
            fos = new FileOutputStream(file);
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
        return fos;
    }

    @Override
    public void uploadSucceeded(final Upload.SucceededEvent event) {
//        event.getUpload().setVisible(false);
        lblUpload.setVisible(true);
        if (file.length() > 0) {
            try {
                config.setXmlFile(file);
            } catch (Exception e) {
                e.printStackTrace();
                ElzaNotifications.showError(e.getMessage());
            }
        }
    }

    public AxForm<XmlImportConfig> formularNewFA() {
        config = new XmlImportConfig();
        config.setImportDataFormat(ImportDataFormat.ELZA);

        AxForm<XmlImportConfig> form = AxForm.init(XmlImportConfig.class);
        form.addStyleName("form");

        form.addField("Zastavit při chybě", "stopOnError").required();
        form.setValue(config);

        upload = new Upload("Xml soubor", this);
        upload.addSucceededListener(this);
        upload.addFailedListener(this);



        ruleSetContainer = new AxContainer<>(RulRuleSet.class).supplier(ruleSetManager::getRuleSets);
        ruleSetContainer.setBeanIdProperty("ruleSetId");
        AxForm<AxComboBox>.AxField<AxComboBox> ruleSetCombo = form
                .addCombo("Pravidla tvorby", "ruleSetId", ruleSetContainer, RulRuleSet::getName).required();
        ruleSetCombo.field().addValueChangeListener((event) -> {
            arTypeContainer.refresh();
        });

        arTypeContainer = new AxContainer<>(RulArrangementType.class).supplier((repository) -> {
            Integer ruleSetId = (Integer) ruleSetCombo.field().getValue();
            if (ruleSetId == null) {
                return new ArrayList<RulArrangementType>();
            } else {
                return ruleSetManager.getArrangementTypes(ruleSetId);
            }
        });
        arTypeContainer.addAll(new ArrayList<RulArrangementType>());
        arTypeContainer.setBeanIdProperty("arrangementTypeId");
        form.addCombo("Typ výstupu", "arrangementTypeId", arTypeContainer, RulArrangementType::getName).required();

        form.addComponent(upload);
        lblUpload = newLabel("Soubor uploadován");
        form.addComponent(lblUpload);
        lblUpload.setVisible(false);

        form.validator(new AxForm.AxFormValidator<XmlImportConfig>() {
            @Override
            public void validate(final AxForm<XmlImportConfig> axForm) throws Validator.InvalidValueException {
                if(config.getXmlFile() == null){
                    throw new Validator.InvalidValueException("Není vybrán soubor.");
                }
            }
        });

        return form;
    }
}
