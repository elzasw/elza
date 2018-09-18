package cz.tacr.elza.bulkaction.generator.unitid;

import org.apache.commons.lang3.Validate;

public class PartSealedUnitId extends SealedUnitId
{
    final UnitIdPart part;

    final SealedLevel level;

    /**
     * Flag if value is sealed
     */
    boolean sealed = false;

    public PartSealedUnitId(UnitIdPart part, SealedLevel level) {
        Validate.notNull(level);
        Validate.notNull(part);

        this.part = part;
        this.level = level;
    }

    public UnitIdPart getPart() {
        return part;
    }

    public void setSealed(boolean b) {
        sealed = b;
    }

    public boolean isSealed() {
        return sealed;
    }

    @Override
    public int getDepth() {
        return level.getDepth() + 1;
    }

    public String getValue() {
        StringBuilder sb = new StringBuilder();
        printValue(sb);
        return sb.toString();
    }

    @Override
    public void printValue(StringBuilder sb) {
        level.printValue(sb);
        sb.append(part.toString());
    }

}
