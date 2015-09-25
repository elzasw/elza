package cz.tacr.elza.ui.window;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import cz.req.ax.AxWindow;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.vo.ArrNodeHistoryItem;
import cz.tacr.elza.domain.vo.ArrNodeHistoryPack;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 18.8.2015
 */
public class LevelHistoryWindow extends AxWindow {

    private ArrangementManager arrangementManager;

    public LevelHistoryWindow(final ArrangementManager arrangementManager) {
        Assert.notNull(arrangementManager);

        this.arrangementManager = arrangementManager;
    }

    public AxWindow show(final ArrLevel level, final Integer findingAidId) {
        List<CssLayout> componentList = createVersionHistory(level, findingAidId);
        CssLayout layout = new CssLayout();
        layout.addStyleName("history-box");
        layout.setSizeUndefined();
        for (CssLayout cssLayout : componentList) {
            layout.addComponent(cssLayout);
        }

        caption("Historie změn uzlu").components(layout)
                .buttonClose().modal().style("history-window-detail");

        return super.show();
    }


    private List<CssLayout> createVersionHistory(final ArrLevel level, final Integer findingAidId) {
        List<CssLayout> resultList = new ArrayList<>();

        ArrNodeHistoryPack nodeHistoryPack = arrangementManager.getHistoryForNode(level.getNode().getNodeId(), findingAidId);

        Map<Integer, List<ArrNodeHistoryItem>> items = nodeHistoryPack.getItems();

        List<ArrFindingAidVersion> versions = arrangementManager.getFindingAidVersions(findingAidId);

        Collections.sort(versions, (o1, o2) -> o1.getCreateChange().getChangeDate().compareTo(o2.getCreateChange().getChangeDate()));

        for (ArrFindingAidVersion version : versions) {
            List<ArrNodeHistoryItem> nodeHistoryItems = items.get(version.getFindingAidVersionId());
            if (nodeHistoryItems.size() > 0) {
                CssLayout layout = createVersionHistoryItem(version, nodeHistoryItems);
                resultList.add(layout);
            }
        }

        return resultList;
    }

    private CssLayout createVersionHistoryItem(ArrFindingAidVersion findingAidVersion, List<ArrNodeHistoryItem> nodeHistoryItems) {

        CssLayout layout = new CssLayout();
        layout.setSizeUndefined();
        layout.setStyleName("version-header");

        String lockDataStr = "aktuální";
        if (findingAidVersion.getLockChange() != null) {
            lockDataStr = "uzavřena k " + findingAidVersion.getLockChange().getChangeDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        }
        Label header = newLabel(findingAidVersion.getFindingAidVersionId() + " - " + lockDataStr, "h2");
        layout.addComponent(header);

        if (nodeHistoryItems.isEmpty()) {
            layout.addComponent(cssLayout("", newLabel("Žádné změny")));
        } else {

            layout.addComponent(createVersionHistoryHeader());

            Collections.sort(nodeHistoryItems, (o1, o2) -> o1.getChange().getChangeDate().compareTo(o2.getChange().getChangeDate()));

            Label text;
            CssLayout layoutRow;

            for (ArrNodeHistoryItem nodeHistoryItem : nodeHistoryItems) {

                Component componentAkce;

                // pouze z důvodu načtení objektů do sessions
                List<ArrDescItem> descItems = nodeHistoryItem.getDescItems();
                if (descItems != null) {
                    for (ArrDescItem descItem : descItems) {
                        if (descItem.getDescItemSpec() != null) {
                            descItem.getDescItemSpec().getName().toString();
                        }
                        descItem.toString();
                    }
                }
                // konec

                switch (nodeHistoryItem.getType()) {
                    case LEVEL_CREATE:
                        componentAkce = newLabel("Vytvoření uzlu");
                        break;
                    case LEVEL_DELETE:
                        componentAkce = newLabel("Smazání uzlu");
                        break;
                    case LEVEL_CHANGE:
                        componentAkce = newLabel("Změna zařazení uzlu");
                        break;
                    case ATTRIBUTE_CHANGE:
                        text = newLabel("Změna atributů");
                        text.addStyleName("link");
                        layoutRow = new CssLayout(text);
                        layoutRow.addLayoutClickListener(event -> {
                            showDetail(nodeHistoryItem);
                        });
                        componentAkce = layoutRow;
                        break;
                    default:
                        throw new IllegalStateException("Nedefinovaný typ akce");
                }

                String createDataStr = nodeHistoryItem.getChange().getChangeDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
                Label labelChangeDate = newLabel(createDataStr);
                CssLayout layoutChildren = new CssLayout(labelChangeDate, componentAkce);
                layout.addComponent(layoutChildren);

            }

        }

        return layout;
    }

    private CssLayout createVersionHistoryHeader() {
        CssLayout layout = cssLayout("version-table-header", newLabel("Datum změny"), newLabel("Akce"));
        return layout;
    }

    private LevelHistoryDetailWindow showDetail(ArrNodeHistoryItem nodeHistoryItem) {
        LevelHistoryDetailWindow window = new LevelHistoryDetailWindow(arrangementManager);
        window.show(nodeHistoryItem);
        return window;
    }
}
