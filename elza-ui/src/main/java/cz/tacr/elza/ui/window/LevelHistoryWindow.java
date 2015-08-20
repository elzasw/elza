package cz.tacr.elza.ui.window;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.util.Assert;

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import cz.req.ax.AxWindow;
import cz.tacr.elza.controller.ArrangementManager;
import cz.tacr.elza.domain.ArrFaLevel;
import cz.tacr.elza.domain.ArrFaVersion;


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

    public AxWindow show(final Integer nodeId, final Integer findingAidId) {
        List<CssLayout> componentList = createVersionHistory(nodeId, findingAidId);
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


    private List<CssLayout> createVersionHistory(final Integer nodeId, final Integer findingAidId) {
            final List<ArrFaVersion> versionList = arrangementManager.getFindingAidVersions(findingAidId);
            List<ArrFaLevel> allLevels = arrangementManager.findLevels(nodeId);
            List<CssLayout> resultList = new ArrayList<>();

            final Map<ArrFaVersion, List<ArrFaLevel>> versionMap = prepareVersionLevelMap(nodeId, findingAidId);
            ArrFaLevel firstLevel = allLevels.get(0);
            ArrFaLevel lastLevel = allLevels.get(allLevels.size()-1);


            int versionNumber = 0;
            for (ArrFaVersion version : versionList) {
                versionNumber++;

                List<ArrFaLevel> levels = versionMap.get(version);
                if(levels == null || levels.isEmpty()) {
                    continue;
                }

                CssLayout layout = createVersionHistoryItem(versionNumber, version, levels, firstLevel, lastLevel);
                resultList.add(layout);
            }

            return resultList;
        }

        private Map<ArrFaVersion, List<ArrFaLevel>> prepareVersionLevelMap(final Integer nodeId, final Integer findingAidId) {
            final List<ArrFaLevel> levelList = arrangementManager.findLevels(nodeId);
            final List<ArrFaVersion> versionList = arrangementManager.getFindingAidVersions(findingAidId);


            ArrFaVersion[] versions = new ArrFaVersion[versionList.size()];
            Integer[] versionEnds = new Integer[versionList.size()];
            final Map<ArrFaVersion, List<ArrFaLevel>> versionMap = new LinkedHashMap<ArrFaVersion, List<ArrFaLevel>>();

            int index = 0;
            for (ArrFaVersion faVersion : versionList) {
                versionEnds[index] = faVersion.getLockChange() == null ? Integer.MAX_VALUE
                                                                       : faVersion.getLockChange().getChangeId();
                versions[index] = faVersion;
                index++;
            }

            boolean firstLevel = true;
            for (ArrFaLevel faLevel : levelList) {
                ArrFaVersion version = getVersionByChangeId(firstLevel, faLevel, versionEnds, versions);
                firstLevel = false;

                List<ArrFaLevel> levels = versionMap.get(version);
                if (levels == null) {
                    levels = new LinkedList<ArrFaLevel>();
                    versionMap.put(version, levels);
                }
                levels.add(faLevel);
            }

            return versionMap;
        }

        private ArrFaVersion getVersionByChangeId(final boolean firstLevel,
                                               @Nullable final ArrFaLevel faLevel,
                                               final Integer[] versionEnds,
                                               final ArrFaVersion[] versions) {
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
                                                   ArrFaVersion faVersion,
                                                   List<ArrFaLevel> levelSublist,
                                                   ArrFaLevel firstLevel,
                                                   ArrFaLevel lastLevel) {
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
                for (ArrFaLevel faLevel : levelSublist) {
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

                    String parentNodeId = (faLevel.getParentNodeId() == null) ? "" : faLevel.getParentNodeId().toString();
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
