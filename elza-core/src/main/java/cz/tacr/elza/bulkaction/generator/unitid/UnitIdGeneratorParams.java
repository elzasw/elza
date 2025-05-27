package cz.tacr.elza.bulkaction.generator.unitid;

import cz.tacr.elza.bulkaction.BulkActionTransactional;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;

public class UnitIdGeneratorParams {
    final BulkActionTransactional bulkAction;
    final RulItemType itemType;
    final RulItemType levelItemType;
    final SealedUnitIdTree sealedUnitIdTree;
    final RulItemSpec extraSlashLevelType;
    final RulItemType prevItemType;
    final RulItemSpec prevItemSpec;

    public UnitIdGeneratorParams(BulkActionTransactional bulkAction, RulItemType itemType, RulItemType levelItemType,
            SealedUnitIdTree sealedUnitIdTree, RulItemSpec extraSlashLevelType,
            RulItemType prevItemType,
            RulItemSpec prevItemSpec) {
        this.bulkAction = bulkAction;
        this.itemType = itemType;
        this.levelItemType = levelItemType;
        this.sealedUnitIdTree = sealedUnitIdTree;
        this.extraSlashLevelType = extraSlashLevelType;
        this.prevItemType = prevItemType;
        this.prevItemSpec = prevItemSpec;
    }

    public BulkActionTransactional getBulkAction() {
        return bulkAction;
    }

    public RulItemType getItemType() {
        return itemType;
    }

    public RulItemType getLevelItemType() {
        return levelItemType;
    }

    public SealedUnitIdTree getSealedTree() {
        return sealedUnitIdTree;
    }

    public RulItemSpec getExtraSlashLevelType() {
        return extraSlashLevelType;
    }

    public RulItemType getPreviousItemType() {
        return prevItemType;
    }

    public RulItemSpec getPreviousItemSpec() {
        return prevItemSpec;
    }
}
