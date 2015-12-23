package cz.tacr.elza.ui.view;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import cz.tacr.elza.controller.XmlImportManager;
import cz.tacr.elza.ui.window.PackagesWindow;
import cz.tacr.elza.ui.window.XmlImportWindow;
import ru.xpoft.vaadin.VaadinView;

import com.vaadin.data.Validator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Table;

import cz.req.ax.AxAction;
import cz.req.ax.AxBeanTable;
import cz.req.ax.AxComboBox;
import cz.req.ax.AxContainer;
import cz.req.ax.AxForm;
import cz.req.ax.AxTable;
import cz.req.ax.AxWindow;
import cz.req.ax.util.LocalDateTimeConverter;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.controller.RuleManager;
import cz.tacr.elza.domain.ArrFindingAid;
import cz.tacr.elza.domain.RulArrangementType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.ui.ElzaView;
import cz.tacr.elza.ui.utils.ConcurrentUpdateExceptionHandler;


/**
 * Seznam archivnívh pomůcek.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 7. 2015
 */
@Component
@Scope("prototype")
@VaadinView("FindingAidList")
public class FindingAidListView extends ElzaView {

    @Autowired
    private ArrangementManager arrangementManager;

    @Autowired
    private RuleManager ruleSetManager;

    @Autowired
    private XmlImportManager xmlImportManager;

    AxContainer<RulArrangementType> arTypeContainer;
    AxContainer<RulRuleSet> ruleSetContainer;
    AxTable<ArrFindingAid> tableFA;
    AxForm<ArrFindingAid> formFA;

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);

        pageTitle("Archivní pomůcky");

        formFA = formularFA();

        tableFA = new AxBeanTable<>(AxContainer.init(ArrFindingAid.class).supplier(arrangementManager::getFindingAids));
        tableFA.style("v-selectable");
        tableFA.select(findingAid -> navigate(FindingAidDetailView.class, findingAid.getFindingAidId()));
        tableFA.header(Table.ColumnHeaderMode.EXPLICIT_DEFAULTS_ID)
        .column("name").header("Název")
        .column("createDate").header("Datum vytvoření").width(200).converter(new LocalDateTimeConverter())
        .column("actions").header("Akce").width(100).generator((itemObject, itemId, columnId) ->
        cssLayout("action-transparent",
                AxAction.of(itemObject).icon(FontAwesome.EDIT).action(this::upravitFA).button(),
                AxAction.of(itemObject).icon(FontAwesome.TIMES).action(this::smazatFA).button()
                )).done();

        actions(new AxAction().caption("Nový").icon(FontAwesome.PLUS_CIRCLE)
                        .run(() -> novyFA(formularNewFA())),
                new AxAction().caption("Packages").icon(FontAwesome.PLUS_CIRCLE)
                        .run(() -> showPackagesWindow()),
                new AxAction().caption("Testovací data").icon(FontAwesome.DATABASE)
                        .run(() -> navigate(TestingDataView.class)),
                new AxAction().caption("Xml import").icon(FontAwesome.UPLOAD).run(() -> createXmlImportWindow())
        );

        components(tableFA.getTable());
        refresh();
    }

    private void refresh() {
        tableFA.refresh();
    }

    private void novyFA(final AxForm<VONewFindingAid> form) {
        new AxWindow().caption("Vytvoření archivní pomůcky").components(form).buttonClose().buttonPrimary(
                new AxAction<VONewFindingAid>().caption("Uložit")
                .value(form::commit).action(this::vytvoritFA)
                .exception(ex -> ex.printStackTrace())
                ).modal().style("window-detail").show();
    }

    private PackagesWindow showPackagesWindow() {
        PackagesWindow window = new PackagesWindow(ruleSetManager);
        window.show();
        return window;
    }

    private void upravitFA(final ArrFindingAid findingAid) {
        formFA.setValue(findingAid);
        new AxWindow().caption("Úprava archivní pomůcky").components(formFA).buttonClose().buttonPrimary(
                new AxAction<ArrFindingAid>().caption("Uložit")
                .value(formFA::commit).action(this::ulozitFA)
                .exception(new ConcurrentUpdateExceptionHandler())
                ).modal().style("window-detail").show();
    }

    private void vytvoritFA(final VONewFindingAid findingAid) {
        arrangementManager.createFindingAid(findingAid.getName(), findingAid.getArrangementTypeId(),
                findingAid.getRuleSetId());
        refresh();
    }

    private void ulozitFA(final ArrFindingAid findingAid) {
        arrangementManager.updateFindingAid(findingAid);
        refresh();
    }

    private void smazatFA(final ArrFindingAid findingAid) {
        new AxWindow().caption("Smazání archivní pomůcky")
                .components(
                        newLabel("Opravdu chcete smazat archivní pomůcku s názvem \"" + findingAid.getName() + "\""))
                .buttonClose().buttonPrimary(new AxAction<VONewFindingAid>().caption("Smazat")
                        .action((e) -> {
                            arrangementManager.deleteFindingAid(findingAid.getFindingAidId());
                            refresh();
                        })
                        .exception(ex -> ex.printStackTrace())
        ).modal().style("window-detail").show();
    }

    @Bean
    @Scope("prototype")
    @Qualifier("formularFA")
    AxForm<ArrFindingAid> formularFA() {
        AxForm<ArrFindingAid> form = AxForm.init(ArrFindingAid.class);
        form.addStyleName("form");
        form.addField("Název", "name").required();
        return form;
    }


    @Bean
    @Scope("prototype")
    @Qualifier("formularNewFA")
    AxForm<VONewFindingAid> formularNewFA() {
        AxForm<VONewFindingAid> form = AxForm.init(VONewFindingAid.class);
        form.addStyleName("form");
        form.addField("Název", "name").required().validator(value -> {
            String val = (String) value;
            if (StringUtils.isEmpty(val)) {
                throw new Validator.InvalidValueException("Název nesmí být prázdný");
            }
        });

        ruleSetContainer = new AxContainer<>(RulRuleSet.class).supplier(ruleSetManager::getRuleSets);
        ruleSetContainer.setBeanIdProperty("ruleSetId");
        AxForm<AxComboBox>.AxField<AxComboBox> ruleSetCombo = form.addCombo("Pravidla tvorby", "ruleSetId", ruleSetContainer, RulRuleSet::getName).required();
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

        return form;
    }



    XmlImportWindow createXmlImportWindow(){

        Runnable refresh = new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        };

        return new XmlImportWindow(ruleSetManager, xmlImportManager, refresh);
    }
}

