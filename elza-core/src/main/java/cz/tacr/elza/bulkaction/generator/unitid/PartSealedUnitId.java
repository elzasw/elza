package cz.tacr.elza.bulkaction.generator.unitid;

import org.apache.commons.lang3.Validate;

public class PartSealedUnitId extends AssignedUnitId
{
    public enum SealType {
        NOT_SEALED,
        // unitId was just assigned
        ASSIGNED,
        // sealed in DB
        FULLY_SEALED
    }
    final UnitIdPart part;

    final SealedLevel level;

    /**
     * Flag if value is sealed
     */
    private SealType sealType = SealType.NOT_SEALED;

    /**
     * Seal validator.
     * 
     * Validator is optional.
     */
    private SealValidator sealValidator = null;

    public PartSealedUnitId(UnitIdPart part, SealedLevel level) {
        Validate.notNull(level);
        Validate.notNull(part);

        this.part = part;
        this.level = level;
    }

    public UnitIdPart getPart() {
        return part;
    }

    /**
     * Set if item is sealed
     * 
     * @param b
     */
    public void setSealed(final SealType sealType, final SealValidator validator) {
        this.sealType = sealType;
        this.sealValidator = validator;
    }

    public boolean isSealed() {
        return sealType != SealType.NOT_SEALED;
    }

    public boolean isFullySealed() {
        return sealType == SealType.FULLY_SEALED;
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

    public void setSealValidator(final SealValidator validator) {
        this.sealValidator = validator;
    }

    public SealValidator getSealValidator() {
        return sealValidator;
    }

}
