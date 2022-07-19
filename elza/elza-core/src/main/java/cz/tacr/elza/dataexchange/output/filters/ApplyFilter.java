package cz.tacr.elza.dataexchange.output.filters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Objects;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.output.sections.LevelInfoImpl;
import cz.tacr.elza.domain.ArrItem;

public class ApplyFilter {

    private boolean hideLevel = false;

    private boolean hideDao = false;

    private Set<ArrItem> hideItems = new HashSet<>();

    private List<ArrItem> addItems = new ArrayList<>();

    public void hideLevel() {
        hideLevel = true;
    }

    public void hideDao() {
        hideDao = true;
    }

    public void addHideItem(ArrItem item) {
        hideItems.add(item);
    }

    public void addItem(ArrItem item) {
        // check if item not exists
        for (ArrItem currItem : addItems) {
            if (!Objects.equal(currItem.getItemTypeId(), item.getItemTypeId())) {
                continue;
            }
            if (!Objects.equal(currItem.getItemSpecId(), item.getItemSpecId())) {
                continue;
            }
            // check data 
            if (Objects.equal(currItem.getData(), item.getData())) {
                return;
            }

            // if enum -> data are not used
            if (currItem.getItemType().getDataTypeId().equals(DataType.ENUM.getEntity().getDataTypeId())) {
                return;
            }
        }
        addItems.add(item);
    }

    public LevelInfoImpl apply(LevelInfoImpl levelInfo) {
        if (hideLevel) {
            return null;
        }

        LevelInfoImpl levelInfoCopy = new LevelInfoImpl(levelInfo);

        if (hideDao) {
            levelInfoCopy.removeDao();
        }

        if (!hideItems.isEmpty()) {
            levelInfoCopy.removeItems(hideItems);
        }

        addItems.forEach(item -> levelInfoCopy.addItem(item));

        return levelInfoCopy;
    }
}
