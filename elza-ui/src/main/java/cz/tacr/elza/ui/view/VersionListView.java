package cz.tacr.elza.ui.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.xpoft.vaadin.VaadinView;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Table;

import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.FaChange;
import cz.tacr.elza.domain.FaVersion;
import cz.tacr.elza.ui.ElzaView;


/**
 * Seznam verzí pro danou archivní pomůcku.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 5. 8. 2015
 */
@Component
@Scope("prototype")
@VaadinView("VersionList")
public class VersionListView extends ElzaView {

    @Autowired
    private ArrangementManager arrangementManager;

    BeanItemContainer<FaVersion> container;

    private Integer findingAidId;

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        super.enter(event);

        findingAidId = getParameterInteger();
        if (findingAidId == null) {
            navigate(FindingAidListView.class);
            return;
        }

        pageTitle("Verze");

        Table table = new Table();
        table.setWidth("100%");
        table.addContainerProperty("createChange", LocalDateTime.class, null, "Datum vytvoření", null, null);
        table.addContainerProperty("lockChange", LocalDateTime.class, null, "Datum uzavření", null, null);
        table.setSortEnabled(false);
        table.addGeneratedColumn("createChange", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                FaVersion version = (FaVersion) itemId;
                return version.getCreateChange().getChangeDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
            }
        });
        table.addGeneratedColumn("lockChange", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                FaVersion version = (FaVersion) itemId;
                FaChange lockChange = version.getLockChange();
                if (lockChange == null) {
                    return null;
                }
                return lockChange.getChangeDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
            }
        });

        table.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(final ItemClickEvent itemClickEvent) {
                FaVersion version = (FaVersion) itemClickEvent.getItemId();
                Integer versionId = null;
                if (version.getLockChange() != null) {
                    versionId = version.getFaVersionId();
                }
                VOFindingAidVersionParam params = new VOFindingAidVersionParam(findingAidId, versionId);
                navigate(FindingAidDetailView.class, params);
            }
        });

        container = new BeanItemContainer<>(FaVersion.class);

        container.addAll(arrangementManager.getFindingAidVersions(findingAidId));

        table.addStyleName("table");
        table.setContainerDataSource(container);
        table.setVisibleColumns("createChange", "lockChange");

        components(table);
    }

}
