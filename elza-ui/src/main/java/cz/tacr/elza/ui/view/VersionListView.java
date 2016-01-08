package cz.tacr.elza.ui.view;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.Table;

import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.repository.VersionConformityRepository;
import cz.tacr.elza.ui.ElzaView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.xpoft.vaadin.VaadinView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


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

    private static final String ID_TABLE = "idTable";

    @Autowired
    private ArrangementManager arrangementManager;

    @Autowired
    private VersionConformityRepository findingAidVersionConformityInfoRepository;

    BeanItemContainer<ArrFindingAidVersion> container;

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
        table.setColumnHeader(ID_TABLE, "Pořadí");
        table.addContainerProperty("state", cz.tacr.elza.api.ArrVersionConformity.State.class, "Neznámý");
        table.addContainerProperty("createChange", LocalDateTime.class, null, "Datum vytvoření", null, null);
        table.addContainerProperty("lockChange", LocalDateTime.class, null, "Datum uzavření", null, null);
        table.setSortEnabled(false);
        AtomicInteger poradi = new AtomicInteger(0);
        table.addGeneratedColumn("state", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table table, final Object itemId, final Object colId) {
                ArrFindingAidVersion version = (ArrFindingAidVersion) itemId;
                cz.tacr.elza.domain.ArrVersionConformity conformityInfo = findingAidVersionConformityInfoRepository
                        .findByVersion(version);
                return conformityInfo == null || conformityInfo.getState() == null ? "Neznámý" : conformityInfo.getState().name();
            }
        });
        table.addGeneratedColumn(ID_TABLE, new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                return Integer.valueOf(poradi.incrementAndGet());
            }
        });
        table.addGeneratedColumn("createChange", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                ArrFindingAidVersion version = (ArrFindingAidVersion) itemId;
                return version.getCreateChange().getChangeDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
            }
        });
        table.addGeneratedColumn("lockChange", new Table.ColumnGenerator() {
            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                ArrFindingAidVersion version = (ArrFindingAidVersion) itemId;
                ArrChange lockChange = version.getLockChange();
                if (lockChange == null) {
                    return null;
                }
                return lockChange.getChangeDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
            }
        });

        table.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(final ItemClickEvent itemClickEvent) {
                ArrFindingAidVersion version = (ArrFindingAidVersion) itemClickEvent.getItemId();
                Integer versionId = null;
                if (version.getLockChange() != null) {
                    versionId = version.getFindingAidVersionId();
                }
                VOFindingAidVersionParam params = new VOFindingAidVersionParam(findingAidId, versionId);
                navigate(FindingAidDetailView.class, params);
            }
        });

        container = new BeanItemContainer<>(ArrFindingAidVersion.class);

        List<ArrFindingAidVersion> dataSource = arrangementManager.getFindingAidVersions(findingAidId);
        sortFa(dataSource);
        container.addAll(dataSource);

        table.addStyleName("table");
        table.setContainerDataSource(container);
        table.setVisibleColumns(ID_TABLE,"state", "createChange", "lockChange");

        components(table);
    }

    /**
     * seřadí fa vertion list podle data vytvoření.
     * @param dataSource
     */
    private void sortFa(final List<ArrFindingAidVersion> dataSource) {
        Comparator<ArrFindingAidVersion> comparator = (e1, e2) -> e1.getCreateChange().getChangeDate()
                .compareTo(e2.getCreateChange().getChangeDate());
        Collections.sort(dataSource, comparator);
    }
}
