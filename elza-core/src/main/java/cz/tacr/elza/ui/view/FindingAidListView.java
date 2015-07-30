package cz.tacr.elza.ui.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.vaadin.data.Validator;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;

import cz.tacr.elza.ax.AxAction;
import cz.tacr.elza.ax.AxForm;
import cz.tacr.elza.ax.AxWindow;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.FindingAid;
import cz.tacr.elza.ui.ElzaView;
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

    BeanItemContainer<FindingAid> container;

    AxForm<FindingAid> formFA;

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {

        formFA = formularFA();

        Table table = new Table();
//        table.setCaption("Archivní pomůcky");
        table.addContainerProperty("name", String.class, "", "Název", null, null);
        table.addContainerProperty("createDate", LocalDateTime.class, null, "Datum vytvoření", null, null);
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
                    public void buttonClick(Button.ClickEvent event) {
                        upravitFA(formFA, findingAid);
                    }
                });

                Button buttonSmazat = new Button("Smazat");
                buttonSmazat.addStyleName("fa-button-remove");
                buttonSmazat.addClickListener(new Button.ClickListener() {
                    public void buttonClick(Button.ClickEvent event) {
                        smazatFA(findingAid);
                    }
                });

                CssLayout layout = new CssLayout(buttonUpravit, buttonSmazat);
                return layout;
            }
        });

        table.setColumnHeader("actions", "Akce");

        container = new BeanItemContainer<>(FindingAid.class);

        refresh();

        table.addStyleName("fa-table");
        table.setContainerDataSource(container);
        table.setVisibleColumns("name", "createDate", "actions");

        Button button = new Button("Nový");
        button.addStyleName("fa-button-new");
        button.addClickListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                AxForm<FindingAid> form = formularFA();
                novyFA(form);
            }
        });

        Label elza = new Label("ELZA");
        elza.addStyleName("fa-header-elza");
        CssLayout headerBar = new CssLayout(elza, button);
        headerBar.addStyleName("fa-header");
        addComponent(headerBar);

        Label title = new Label("<h1>Archivní pomůcky</h1>");
        title.setContentMode(ContentMode.HTML);
        CssLayout titleBar = new CssLayout(title);
        titleBar.addStyleName("fa-title");
        addComponent(titleBar);

        addComponent(table);


    }

    private void refresh() {
        container.removeAllItems();
        container.addAll(arrangementManager.getFindingAids());
    }

    private void novyFA(AxForm<FindingAid> form) {
        new AxWindow().components(form).buttonClose()
                .buttonPrimary(new AxAction<FindingAid>()
                                .caption("Uložit")
                                .primary()
                                .exception(ex -> {
                                    ex.printStackTrace();
                                })
                                .value(form::commit)
                                .action(this::vytvoritFA)
                ).modal().style("fa-window-detail").show();
    }

    private void upravitFA(AxForm<FindingAid> form, FindingAid findingAid) {
        form.setValue(findingAid);
        new AxWindow().components(form).buttonClose()
                .buttonPrimary(new AxAction<FindingAid>()
                                .caption("Uložit")
                                .exception(ex -> {
                                    ex.printStackTrace();
                                })
                                .primary()
                                .value(form::commit)
                                .action(this::ulozitFA)
                ).modal().style("fa-window-detail").show();
    }

    private void vytvoritFA(FindingAid findingAid) {
        arrangementManager.createFindingAid(findingAid.getName());
        refresh();
    }

    private void ulozitFA(FindingAid findingAid) {
        arrangementManager.updateFindingAid(findingAid.getFindigAidId(), findingAid.getName());
        refresh();
    }

    private void smazatFA(FindingAid findingAid) {
        arrangementManager.deleteFindingAid(findingAid.getFindigAidId());
        refresh();
    }

    @Bean
    @Scope("prototype")
    @Qualifier("formularFA")
    AxForm<FindingAid> formularFA() {
        AxForm<FindingAid> form = AxForm.init(FindingAid.class);
        form.addStyleName("fa-form");
        form.addField("Název", "name").required().validator(value -> {
            String val = (String) value;
            if (StringUtils.isEmpty(val)) {
                throw new Validator.InvalidValueException("Název nesmí být prázdný");
            }
        });
        return form;
    }

}

