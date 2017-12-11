package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrItem;

public class RootLevelDecorator implements ExportLevelInfoListener {

    private final List<ArrItem> extraItems;

    public RootLevelDecorator(Collection<? extends ArrItem> extraItems) {
        this.extraItems = new ArrayList<>(extraItems);
    }

    @Override
    public void onInit(ExportLevelInfo levelInfo) {
        if (levelInfo.getParentNodeId() == null) {
            extraItems.forEach(levelInfo::addItem);
        }
    }
}
