package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrItem;

public class RootLevelDecorator implements LevelInfoListener {

    private final List<ArrItem> additionalItems;

    public RootLevelDecorator(Collection<? extends ArrItem> additionalItems) {
        this.additionalItems = new ArrayList<>(additionalItems);
    }

    @Override
    public void onInit(LevelInfoImpl levelInfo) {
        if (levelInfo.getParentNodeId() == null) {
            additionalItems.forEach(levelInfo::addItem);
        }
    }
}
