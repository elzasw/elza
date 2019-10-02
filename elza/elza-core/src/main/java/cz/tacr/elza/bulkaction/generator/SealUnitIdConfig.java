package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkAction;

/**
 * Configuration of bulk action to seal UnitId
 * 
 * Action will check format of UnitId and store
 * used values in ArrUsedValue table
 */
public class SealUnitIdConfig
        extends BaseActionConfig {

    /**
     * Item type of UnitId
     */
    String itemType;

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    @Override
    public BulkAction createBulkAction() {
        SealUnitId action = new SealUnitId(this);
        return action;
    }

}
