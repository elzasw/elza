package cz.tacr.elza.bulkaction.generator.unitid;

import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;

public class UnitIdGeneratorParams {
    final BulkAction bulkAction;
    final RulItemType itemType;
    final RulItemType levelItemType;
    final SealedUnitIdTree sealedUnitIdTree;
    final RulItemSpec extraSlashLevelType;
    final RulItemType prevItemType;
    final RulItemSpec prevItemSpec;

    public UnitIdGeneratorParams(BulkAction bulkAction, RulItemType itemType, RulItemType levelItemType,
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

    public BulkAction getBulkAction() {
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
