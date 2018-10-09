package cz.tacr.elza.bulkaction.generator.unitid;

public class SealedUnitIdTree extends SealedUnitId {

    public SealedUnitIdTree() {
        // empty constructor for easier debugging
    }

    public PartSealedUnitId addSealedValue(String uv, SealValidator validator) throws UnitIdException {
        return addValue(uv, true, validator);
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
