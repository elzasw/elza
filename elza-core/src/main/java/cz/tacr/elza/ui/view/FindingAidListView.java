package cz.tacr.elza.ui.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import cz.tacr.elza.repository.LevelRepository;
import ru.xpoft.vaadin.VaadinView;

import com.vaadin.data.Validator;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;

import cz.req.ax.AxAction;
import cz.req.ax.AxContainer;
import cz.req.ax.AxForm;
import cz.req.ax.AxWindow;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.controller.RuleSetManager;
import cz.tacr.elza.domain.ArrangementType;
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.domain.RuleSet;
import cz.tacr.elza.ui.ElzaView;


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


    BeanItemContainer<FindingAid> container;
    AxContainer<ArrangementType> arTypeContainer;
    AxContainer<RuleSet> ruleSetContainer;

    AxForm<FindingAid> formFA;

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);

        formFA = formularFA();

        Table table = new Table();
        table.setWidth("100%");
        table.addContainerProperty("name", String.class, "", "Název", null, null);
        table.addContainerProperty("createDate", LocalDateTime.class, null, "Datum vytvoření", null, null);
        table.setSortEnabled(false);
        table.addGeneratedColumn("createDate", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                FindingAid findingAid = (FindingAid) itemId;
                return findingAid.getCreateDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
            }
        });

        table.addGeneratedColumn("actions", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {

                final FindingAid findingAid = (FindingAid) itemId;

                Button buttonUpravit = new Button("Upravit");
                buttonUpravit.addStyleName("fa-button-edit");
                buttonUpravit.addClickListener(new Button.ClickListener() {
                    @Override
                    public void buttonClick(final Button.ClickEvent event) {
                        upravitFA(formFA, findingAid);
                    }
                });

                Button buttonSmazat = new Button("Smazat");
                buttonSmazat.addStyleName("fa-button-remove");
                buttonSmazat.addClickListener(new Button.ClickListener() {
                    @Override
                    public void buttonClick(final Button.ClickEvent event) {
                        smazatFA(findingAid);
                    }
                });

                CssLayout layout = new CssLayout(buttonUpravit, buttonSmazat);
                return layout;
            }
        });

        table.setColumnHeader("actions", "Akce");

        table.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent itemClickEvent) {
                FindingAid findingAid = (FindingAid) itemClickEvent.getItemId();
                navigate(FindingAidDetailView.class, findingAid.getFindigAidId());
            }
        });

        container = new BeanItemContainer<>(FindingAid.class);

        refresh();

        table.addStyleName("fa-table");
        table.setContainerDataSource(container);
        table.setVisibleColumns("name", "createDate", "actions");

        Button button = new Button("Nový");
        button.addStyleName("fa-button-new");
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                AxForm<VONewFindingAid> form = formularNewFA();
                novyFA(form);
            }
        });

        actionsBar().addComponent(button);
        addBodyHead();
        bodyMain().addComponent(table);
    }

    private void refresh() {
        container.removeAllItems();
        container.addAll(arrangementManager.getFindingAids());
    }

    public void addBodyHead() {
        Label title = new Label("<h1>Archivní pomůcky</h1>");
        title.setContentMode(ContentMode.HTML);
        CssLayout titleBar = new CssLayout(title);
        titleBar.addStyleName("fa-title");
        bodyHeadMain().addComponent(titleBar);
    }

    private void novyFA(final AxForm<VONewFindingAid> form) {
        new AxWindow().components(form)
        .buttonPrimary(new AxAction<VONewFindingAid>()
                .caption("Uložit")
                .primary()
                .exception(ex -> {
                    ex.printStackTrace();
                })
                .value(form::commit)
                .action(this::vytvoritFA)
                ).buttonClose().modal().style("fa-window-detail").show();
    }

    private void upravitFA(final AxForm<FindingAid> form, final FindingAid findingAid) {
        form.setValue(findingAid);
        new AxWindow().components(form)
        .buttonPrimary(new AxAction<FindingAid>()
                .caption("Uložit")
                .exception(ex -> {
                    ex.printStackTrace();
                })
                .primary()
                .value(form::commit)
                .action(this::ulozitFA)
                ).buttonClose().modal().style("fa-window-detail").show();
    }

    private void vytvoritFA(final VONewFindingAid findingAid) {
        arrangementManager.createFindingAid(findingAid.getName(), findingAid.getArrangementTypeId(),
                findingAid.getRuleSetId());
        refresh();
    }

    private void ulozitFA(final FindingAid findingAid) {
        arrangementManager.updateFindingAid(findingAid.getFindigAidId(), findingAid.getName());
        refresh();
    }

    private void smazatFA(final FindingAid findingAid) {
        arrangementManager.deleteFindingAid(findingAid.getFindigAidId());
        refresh();
    }

    @Bean
    @Scope("prototype")
    @Qualifier("formularFA")
    AxForm<FindingAid> formularFA() {
        AxForm<FindingAid> form = AxForm.init(FindingAid.class);
        form.addStyleName("fa-form");
        form.setCaption("Úprava archivní pomůcky");
        form.addField("Název", "name").required();
        return form;
    }


    @Bean
    @Scope("prototype")
    @Qualifier("formularNewFA")
    AxForm<VONewFindingAid> formularNewFA() {
        AxForm<VONewFindingAid> form = AxForm.init(VONewFindingAid.class);
        form.addStyleName("fa-form");
        form.setCaption("Vytvoření archivní pomůcky");
        form.addField("Název", "name").required().validator(value -> {
            String val = (String) value;
            if (StringUtils.isEmpty(val)) {
                throw new Validator.InvalidValueException("Název nesmí být prázdný");
            }
        });

        arTypeContainer = new AxContainer<>(ArrangementType.class).supplier(this::getAllArrangementTypes);
        arTypeContainer.setBeanIdProperty("arrangementTypeId");
        form.addCombo("Typ výstupu", "arrangementTypeId", arTypeContainer, ArrangementType::getName).required();

        ruleSetContainer = new AxContainer<>(RuleSet.class).supplier(this::getAllRuleSets);
        ruleSetContainer.setBeanIdProperty("ruleSetId");
        form.addCombo("Pravidla tvorby", "ruleSetId", ruleSetContainer, RuleSet::getName).required();

        return form;
    }

    List<ArrangementType> getAllArrangementTypes() {
        return arrangementManager.getArrangementTypes();
    }

    List<RuleSet> getAllRuleSets() {
        return ruleSetManager.getRuleSets();
    }
}

