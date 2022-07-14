package cz.tacr.elza.dataexchange.output.filters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.tacr.elza.dataexchange.output.sections.LevelInfoImpl;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemType;

public class ApplyFilter {

    private boolean hideLevel = false;

    private Set<ArrItem> hideItems = new HashSet<>();

    private Set<ArrItem> replaceItems = new HashSet<>();

    private RulItemType replaceItemType;

    private List<ArrItem> addedArrItems;

    public void hideLevel() {
        hideLevel = true;
    }

    public void addHideItem(ArrItem item) {
        hideItems.add(item);
    }

    public void addReplaceItem(ArrItem item) {
        replaceItems.add(item);
    }

    public void setReplaceItemType(RulItemType itemType) {
        replaceItemType = itemType;
    }

    public void setAddedArrItems(List<ArrItem> addedArrItems) {
        this.addedArrItems = addedArrItems;
    }

    public LevelInfoImpl apply(LevelInfoImpl levelInfo) {
        if (hideLevel) {
            return null;
        }

        LevelInfoImpl levelInfoCopy = new LevelInfoImpl(levelInfo);

        if (!hideItems.isEmpty()) {
            levelInfoCopy.removeItems(hideItems);
        }

        if (!replaceItems.isEmpty()) {
            for (ArrItem item : replaceItems) {
                item.setItemType(replaceItemType);
            }
        }

        if (addedArrItems != null) {
            levelInfoCopy.addItems(addedArrItems);
        }

        return levelInfoCopy;
    }
}
