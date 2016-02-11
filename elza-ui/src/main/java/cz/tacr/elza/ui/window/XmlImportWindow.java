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
import cz.tacr.elza.api.vo.XmlImportConfig;
import cz.tacr.elza.api.vo.XmlImportType;
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
public class XmlImportWindow extends AxWindow implements Upload.FailedListener, Components {

    private RuleManager ruleSetManager;
    private XmlImportManager xmlImportManager;

    private AxContainer<RulArrangementType> arTypeContainer;
    private AxContainer<RulRuleSet> ruleSetContainer;

    private XmlImportConfig config;
    private File xmlFile;
    private Upload xmlUpload;
    private Label lblXmlUpload;


    private File transformFile;
    private Upload transformUpload;
    private Label lblTransformUpload;

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


    public AxForm<XmlImportConfig> formularNewFA() {
        config = new XmlImportConfig();

        AxForm<XmlImportConfig> form = AxForm.init(XmlImportConfig.class);
        form.addStyleName("form");

        AxForm.AxField importDataFormat = form
                .addCombo("Typ importu", "importDataFormat", XmlImportType.class).required();

        //zastavit při chybě
        form.addField("Zastavit při chybě", "stopOnError").required();
        form.setValue(config);


        //výběr pravidel
        ruleSetContainer = new AxContainer<>(RulRuleSet.class).supplier(ruleSetManager::getRuleSets);
        ruleSetContainer.setBeanIdProperty("ruleSetId");
        AxForm<AxComboBox>.AxField<AxComboBox> ruleSetCombo = form
                .addCombo("Pravidla tvorby", "ruleSetId", ruleSetContainer, RulRuleSet::getName).required();
        ruleSetCombo.field().addValueChangeListener((event) -> {
            arTypeContainer.refresh();
        });


        //Typ výstupu
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
        AxForm<AxComboBox>.AxField<AxComboBox> arrangementTypeCombo = form
                .addCombo("Typ výstupu", "arrangementTypeId", arTypeContainer, RulArrangementType::getName).required();




        //upload xml souboru
        xmlUpload = new Upload("Xml soubor", xmlReceiver());
        xmlUpload.addSucceededListener(xmlSucessListener());
        xmlUpload.addFailedListener(this);

        form.addComponent(xmlUpload);
        lblXmlUpload = newLabel("Soubor nahrán");
        form.addComponent(lblXmlUpload);
        lblXmlUpload.setVisible(false);


        //upload transformačního souboru
        transformUpload = new Upload("Transformační soubor", transformReceiver());
        transformUpload.addSucceededListener(transformSucessListener());
        transformUpload.addFailedListener(this);

        form.addComponent(transformUpload);
        lblTransformUpload = newLabel("Transformační soubor nahrán");
        form.addComponent(lblTransformUpload);
        lblTransformUpload.setVisible(false);


//        importDataFormat.field().addValueChangeListener((event) -> {
//            XmlImportType value = (XmlImportType) importDataFormat.field().getValue();
//            config.setImportDataFormat(value);
//
//            ruleSetCombo.visible(false);
//            ruleSetCombo.field().setRequired(false);
//            arrangementTypeCombo.visible(false);
//            arrangementTypeCombo.field().setRequired(false);
//            transformUpload.setVisible(false);
//
//            if (value != null) {
//                switch (value) {
//                    case ELZA:
//                        break;
//                    case SUZAP:
//                        ruleSetCombo.visible(true);
//                        ruleSetCombo.field().setRequired(true);
//                        arrangementTypeCombo.visible(true);
//                        arrangementTypeCombo.field().setRequired(true);
//                        transformUpload.setVisible(true);
//                        break;
//                    case INTERPI:
//
//                        break;
//                }
//            }
//        });

//        importDataFormat.field().setValue(XmlImportType.ELZA);

        form.validator(new AxForm.AxFormValidator<XmlImportConfig>() {
            @Override
            public void validate(final AxForm<XmlImportConfig> axForm) throws Validator.InvalidValueException {

//                if(config.getImportDataFormat().equals(XmlImportType.INTERPI)){
//                    throw new Validator.InvalidValueException("Není zatím implementováno");
//                }

                if(config.getXmlFile() == null){
                    throw new Validator.InvalidValueException("Není vybrán soubor.");
                }

//                if(config.getImportDataFormat().equals(XmlImportType.SUZAP)){
//                    if(config.getTransformationFile() == null){
//                        throw new Validator.InvalidValueException("Není vybrán transformační soubor.");
//                    }
//                }
            }
        });

        return form;
    }

    private Upload.SucceededListener xmlSucessListener(){
        return new Upload.SucceededListener(){
            @Override
            public void uploadSucceeded(final Upload.SucceededEvent succeededEvent) {
                lblXmlUpload.setVisible(true);
                if (xmlFile.length() > 0) {
                    try {
                        config.setXmlFile(xmlFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                        ElzaNotifications.showError(e.getMessage());
                    }
                }
            }
        };
    }

    private Upload.Receiver xmlReceiver() {
        return new Upload.Receiver() {
            @Override
            public OutputStream receiveUpload(final String s, final String s1) {
                FileOutputStream fos;
                lblXmlUpload.setValue(s);
                try {
                    xmlFile = File.createTempFile("xmlImport", s);
                    xmlFile.deleteOnExit();
                    fos = new FileOutputStream(xmlFile);
                } catch (final Exception e) {
                    e.printStackTrace();
                    return null;
                }
                return fos;
            }
        };
    }


    private Upload.SucceededListener transformSucessListener(){
        return new Upload.SucceededListener(){
            @Override
            public void uploadSucceeded(final Upload.SucceededEvent succeededEvent) {
                lblTransformUpload.setVisible(true);
                if (transformFile.length() > 0) {
                    try {
                        config.setTransformationFile(transformFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                        ElzaNotifications.showError(e.getMessage());
                    }
                }
            }
        };
    }

    private Upload.Receiver transformReceiver() {
        return new Upload.Receiver() {
            @Override
            public OutputStream receiveUpload(final String s, final String s1) {
                FileOutputStream fos;
                lblTransformUpload.setValue(s);
                try {
                    transformFile = File.createTempFile("transformationFile", s);
                    transformFile.deleteOnExit();
                    fos = new FileOutputStream(transformFile);
                } catch (final Exception e) {
                    e.printStackTrace();
                    return null;
                }
                return fos;
            }
        };
    }

}
