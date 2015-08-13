package cz.tacr.elza.ui.view;

import com.vaadin.data.Validator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Table;
import cz.req.ax.*;
import cz.req.ax.util.LocalDateTimeConverter;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.controller.RuleSetManager;
import cz.tacr.elza.domain.ArrangementType;
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.domain.RuleSet;
import cz.tacr.elza.ui.ElzaView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.xpoft.vaadin.VaadinView;


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
    private RuleSetManager ruleSetManager;

    AxContainer<ArrangementType> arTypeContainer;
    AxContainer<RuleSet> ruleSetContainer;
    AxTable<FindingAid> tableFA;
    AxForm<FindingAid> formFA;

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);

        pageTitle("Archivní pomůcky");

        formFA = formularFA();

        tableFA = new AxBeanTable<>(AxContainer.init(FindingAid.class).supplier(arrangementManager::getFindingAids));
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
                .run(() -> novyFA(formularNewFA())));

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

    private void upravitFA(final FindingAid findingAid) {
        formFA.setValue(findingAid);
        new AxWindow().caption("Úprava archivní pomůcky").components(formFA).buttonClose().buttonPrimary(
                new AxAction<FindingAid>().caption("Uložit")
                        .value(formFA::commit).action(this::ulozitFA)
                        .exception(ex -> ex.printStackTrace())
        ).modal().style("window-detail").show();
    }

    private void vytvoritFA(final VONewFindingAid findingAid) {
        arrangementManager.createFindingAid(findingAid.getName(), findingAid.getArrangementTypeId(),
                findingAid.getRuleSetId());
        refresh();
    }

    private void ulozitFA(final FindingAid findingAid) {
        arrangementManager.updateFindingAid(findingAid.getFindingAidId(), findingAid.getName());
        refresh();
    }

    private void smazatFA(final FindingAid findingAid) {
        arrangementManager.deleteFindingAid(findingAid.getFindingAidId());
        refresh();
    }

    @Bean
    @Scope("prototype")
    @Qualifier("formularFA")
    AxForm<FindingAid> formularFA() {
        AxForm<FindingAid> form = AxForm.init(FindingAid.class);
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

        arTypeContainer = new AxContainer<>(ArrangementType.class).supplier(arrangementManager::getArrangementTypes);
        arTypeContainer.setBeanIdProperty("arrangementTypeId");
        form.addCombo("Typ výstupu", "arrangementTypeId", arTypeContainer, ArrangementType::getName).required();

        ruleSetContainer = new AxContainer<>(RuleSet.class).supplier(ruleSetManager::getRuleSets);
        ruleSetContainer.setBeanIdProperty("ruleSetId");
        form.addCombo("Pravidla tvorby", "ruleSetId", ruleSetContainer, RuleSet::getName).required();

        return form;
    }
}

