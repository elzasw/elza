package cz.tacr.elza.ui.window;

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import cz.req.ax.AxWindow;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrLevel;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


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
            final List<ArrFindingAidVersion> versionList = arrangementManager.getFindingAidVersions(findingAidId);
            List<ArrLevel> allLevels = arrangementManager.findLevels(level.getNode().getNodeId());
            List<CssLayout> resultList = new ArrayList<>();

            final Map<ArrFindingAidVersion, List<ArrLevel>> versionMap = prepareVersionLevelMap(level, findingAidId);
            ArrLevel firstLevel = allLevels.get(0);
            ArrLevel lastLevel = allLevels.get(allLevels.size()-1);


            int versionNumber = 0;
            for (ArrFindingAidVersion version : versionList) {
                versionNumber++;

                List<ArrLevel> levels = versionMap.get(version);
                if(levels == null || levels.isEmpty()) {
                    continue;
                }

                CssLayout layout = createVersionHistoryItem(versionNumber, version, levels, firstLevel, lastLevel);
                resultList.add(layout);
            }

            return resultList;
        }

        private Map<ArrFindingAidVersion, List<ArrLevel>> prepareVersionLevelMap(final ArrLevel level, final Integer findingAidId) {
            final List<ArrLevel> levelList = arrangementManager.findLevels(level.getNode().getNodeId());
            final List<ArrFindingAidVersion> versionList = arrangementManager.getFindingAidVersions(findingAidId);


            ArrFindingAidVersion[] versions = new ArrFindingAidVersion[versionList.size()];
            Integer[] versionEnds = new Integer[versionList.size()];
            final Map<ArrFindingAidVersion, List<ArrLevel>> versionMap = new LinkedHashMap<ArrFindingAidVersion, List<ArrLevel>>();

            int index = 0;
            for (ArrFindingAidVersion faVersion : versionList) {
                versionEnds[index] = faVersion.getLockChange() == null ? Integer.MAX_VALUE
                                                                       : faVersion.getLockChange().getChangeId();
                versions[index] = faVersion;
                index++;
            }

            boolean firstLevel = true;
            for (ArrLevel faLevel : levelList) {
                ArrFindingAidVersion version = getVersionByChangeId(firstLevel, faLevel, versionEnds, versions);
                firstLevel = false;

                List<ArrLevel> levels = versionMap.get(version);
                if (levels == null) {
                    levels = new LinkedList<ArrLevel>();
                    versionMap.put(version, levels);
                }
                levels.add(faLevel);
            }

            return versionMap;
        }

        private ArrFindingAidVersion getVersionByChangeId(final boolean firstLevel,
                                               @Nullable final ArrLevel faLevel,
                                               final Integer[] versionEnds,
                                               final ArrFindingAidVersion[] versions) {
            Integer deleteId = faLevel.getDeleteChange() == null ? null : faLevel.getDeleteChange().getChangeId();
            if (firstLevel || deleteId == null) {
                Integer createId = faLevel.getCreateChange().getChangeId();

                int index = Arrays.binarySearch(versionEnds, createId);
                if (index < 0) {
                    index = -index - 1;
                }
                return versions[index];
            } else {
                int index = Arrays.binarySearch(versionEnds, deleteId);
                if (index < 0) {
                    index = -index - 1;
                }
                return versions[index];
            }
        }


        private CssLayout createVersionHistoryItem(final int versionNumber,
                                                   ArrFindingAidVersion faVersion,
                                                   List<ArrLevel> levelSublist,
                                                   ArrLevel firstLevel,
                                                   ArrLevel lastLevel) {
         // pridani verze
            CssLayout layout = new CssLayout();
            layout.setSizeUndefined();
            layout.setStyleName("version-header");
    //        layout.addStyleName("vrItem");


            layout.setSizeUndefined();
            String lockDataStr = "aktuální";
            if (faVersion.getLockChange() != null) {
                lockDataStr = "uzavřena k " + faVersion.getLockChange().getChangeDate()
                        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
            }
            Label header = newLabel(versionNumber+" - "+ lockDataStr, "h2");
            layout.addComponent(header);

            if(levelSublist.isEmpty()){
                layout.addComponent(cssLayout("", newLabel("Žádné změny")));
            }
            else{

                layout.addComponent(createVersionHistoryHeader());

                // pridani levlu
                for (ArrLevel faLevel : levelSublist) {
                    String typZmena = "změna";

                    if (faLevel.equals(lastLevel) && lastLevel.getDeleteChange() != null) {
                        typZmena = "smazání";
                    }


                    if (faLevel.equals(firstLevel)) {
                        typZmena = "vytvoření";
                    }

                    Label labelTyp = newLabel(typZmena);

                    String createDataStr = faLevel.getCreateChange().getChangeDate()
                            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
                    Label labelChangeDate = newLabel(createDataStr);

                    String parentNodeId = (faLevel.getNodeParent() == null) ? "" : faLevel.getNodeParent().getNodeId().toString();
                    Label labelParent = newLabel(parentNodeId);

                    String position = (faLevel.getPosition() == null) ? "" : faLevel.getPosition().toString();
                    Label labelPosition = newLabel(position);

                    CssLayout layoutChildern = new CssLayout(labelTyp, labelChangeDate, labelParent, labelPosition);
                    //            layoutChildern.addStyleName("historie-content");
                    layout.addComponent(layoutChildern);
                }
            }

            return layout;
        }

        private CssLayout createVersionHistoryHeader(){
            CssLayout layout = cssLayout("version-table-header",newLabel("Akce"), newLabel("Datum změny"), newLabel("Nadřazený uzel"),  newLabel("Pozice"));
            return layout;
        }
}
