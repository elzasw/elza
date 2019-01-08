package cz.tacr.elza.bulkaction.generator.unitid;

import cz.tacr.elza.bulkaction.generator.unitid.PartSealedUnitId.SealType;

public class SealedUnitIdTree extends AssignedUnitId {

    public SealedUnitIdTree() {
        // empty constructor for easier debugging
    }

    public PartSealedUnitId addSealedValue(String uv, SealValidator validator) throws UnitIdException {
        return addValue(uv, SealType.FULLY_SEALED, validator);
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public void printValue(StringBuilder sb) {
        // empty implementation - root is not adding any value        
    }

}
