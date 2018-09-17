package cz.tacr.elza.bulkaction.generator.unitid;

public class SealedUnitIdTree extends SealedUnitId {

    public SealedUnitIdTree() {
    }

    public PartSealedUnitId addSealedValue(String uv) throws UnitIdException {
        return addValue(uv, true);
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
