package cz.tacr.elza.ui.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.xpoft.vaadin.VaadinView;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Table;

import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.FindingAid;
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

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        Table table = new Table();
        table.setCaption("Archivní pomůcky");
        table.addContainerProperty("name", String.class, "", "Název", null, null);
        table.addContainerProperty("createDate", LocalDateTime.class, null, "Datum vytvoření", null, null);
        table.addGeneratedColumn("createDate", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                FindingAid findingAid = (FindingAid) itemId;
                return findingAid.getCreateDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
            }
        });

        BeanItemContainer<FindingAid> container = new BeanItemContainer<>(FindingAid.class);
        container.addAll(arrangementManager.getFindingAids());

        table.setContainerDataSource(container);
        table.setVisibleColumns("name", "createDate");
        addComponent(table);
    }
}

