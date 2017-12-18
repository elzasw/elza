package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrItem;

public class ExportRootLevelDecorator implements ExportLevelInfoListener {

    private final List<ArrItem> additionalItems;

    public ExportRootLevelDecorator(Collection<? extends ArrItem> additionalItems) {
        this.additionalItems = new ArrayList<>(additionalItems);
    }

    @Override
    public void onInit(ExportLevelInfo levelInfo) {
        if (levelInfo.getParentNodeId() == null) {
            additionalItems.forEach(levelInfo::addItem);
        }
    }
}
