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
import cz.tacr.elza.domain.FaLevel;
import cz.tacr.elza.domain.FaVersion;


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
            final List<FaVersion> versionList = arrangementManager.getFindingAidVersions(findingAidId);
            List<FaLevel> allLevels = arrangementManager.findLevels(nodeId);
            List<CssLayout> resultList = new ArrayList<>();

            final Map<FaVersion, List<FaLevel>> versionMap = prepareVersionLevelMap(nodeId, findingAidId);
            FaLevel firstLevel = allLevels.get(0);
            FaLevel lastLevel = allLevels.get(allLevels.size()-1);


            int versionNumber = 0;
            for (FaVersion version : versionList) {
                versionNumber++;

                List<FaLevel> levels = versionMap.get(version);
                if(levels == null || levels.isEmpty()) {
                    continue;
                }

                CssLayout layout = createVersionHistoryItem(versionNumber, version, levels, firstLevel, lastLevel);
                resultList.add(layout);
            }

            return resultList;
        }

        private Map<FaVersion, List<FaLevel>> prepareVersionLevelMap(final Integer nodeId, final Integer findingAidId) {
            final List<FaLevel> levelList = arrangementManager.findLevels(nodeId);
            final List<FaVersion> versionList = arrangementManager.getFindingAidVersions(findingAidId);


            FaVersion[] versions = new FaVersion[versionList.size()];
            Integer[] versionEnds = new Integer[versionList.size()];
            final Map<FaVersion, List<FaLevel>> versionMap = new LinkedHashMap<FaVersion, List<FaLevel>>();

            int index = 0;
            for (FaVersion faVersion : versionList) {
                versionEnds[index] = faVersion.getLockChange() == null ? Integer.MAX_VALUE
                                                                       : faVersion.getLockChange().getChangeId();
                versions[index] = faVersion;
                index++;
            }

            boolean firstLevel = true;
            for (FaLevel faLevel : levelList) {
                FaVersion version = getVersionByChangeId(firstLevel, faLevel, versionEnds, versions);
                firstLevel = false;

                List<FaLevel> levels = versionMap.get(version);
                if (levels == null) {
                    levels = new LinkedList<FaLevel>();
                    versionMap.put(version, levels);
                }
                levels.add(faLevel);
            }

            return versionMap;
        }

        private FaVersion getVersionByChangeId(final boolean firstLevel,
                                               @Nullable final FaLevel faLevel,
                                               final Integer[] versionEnds,
                                               final FaVersion[] versions) {
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
                                                   FaVersion faVersion,
                                                   List<FaLevel> levelSublist,
                                                   FaLevel firstLevel,
                                                   FaLevel lastLevel) {
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
                for (FaLevel faLevel : levelSublist) {
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
