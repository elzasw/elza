package cz.tacr.elza.bulkaction.generator.unitid;

import java.util.TreeMap;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.bulkaction.generator.unitid.AssignedUnitId.LevelType;
import cz.tacr.elza.bulkaction.generator.unitid.PartSealedUnitId.SealType;

public class SealedLevel {

    final TreeMap<UnitIdPart, PartSealedUnitId> units = new TreeMap<>();
    final private AssignedUnitId parent;

    final private LevelType levelType;

    SealedLevel(AssignedUnitId parent, LevelType levelType) {
        Validate.notNull(parent);
        this.parent = parent;
        this.levelType = levelType;
    }

    /**
     * 
     * @param part
     * @param remaining
     * @param seal
     *            Flag if value should be marked as sealed
     * @param validator
     * @return
     * @throws UnitIdException
     */
    public PartSealedUnitId add(UnitIdPart part, String remaining, SealType sealType, SealValidator validator)
            throws UnitIdException {
        // check if exists
        PartSealedUnitId unit = units.get(part);
        if (unit == null) {
            unit = new PartSealedUnitId(part, this);
            units.put(part, unit);
        }

        // check empty input
        if (remaining == null || remaining.length() == 0) {
            // existing item cannot be sealed twice
            if (unit.isSealed()) {
                throw new UnitIdException("Item is already sealed");
            }
            unit.setSealed(sealType, validator);
            return unit;
        } else {
            return unit.addValue(remaining, sealType, validator);
        }
    }

    public int getUnitCount() {
        return units.size();
    }

    public PartSealedUnitId getUnit(String value) throws UnitIdException {
        UnitIdPart part = UnitIdPart.parse(value);
        return units.get(part);
    }

    public PartSealedUnitId find(UnitIdPart part, String remaining) {
        // check if exists
        PartSealedUnitId unit = units.get(part);
        if (unit == null) {
            return null;
        }
        if (remaining == null || remaining.length() == 0) {
            return unit;
        }
        return unit.find(remaining);
    }

    public int getDepth() {
        return parent.getDepth();
    }

    /**
     * Try to allocate new unitId
     * 
     * @param loBorder
     *            - allocated id have to be greater then loBoarder
     * @param hiBorder
     *            - allocated id have to be lower then loBoarder
     * @return
     */
    public PartSealedUnitId createSealed(UnitIdPart loBorder, UnitIdPart hiBorder) {

        UnitIdPart unitId = findNotSealed(loBorder, hiBorder);

        PartSealedUnitId result = units.get(unitId);
        if (result == null) {
            result = new PartSealedUnitId(unitId, this);
            units.put(unitId, result);
        } else {
            Validate.isTrue(!result.isSealed());
        }
        // Create sealed based on boarders -> no validator is added
        result.setSealed(SealType.ASSIGNED, null);

        return result;
    }

    private UnitIdPart findNotSealed(UnitIdPart loBorder, UnitIdPart hiBorder) {

        // treat special cases
        if (hiBorder == null) {
            return findFreeHigher(loBorder);
        }

        // no loBorder but exists hiBorder
        if (loBorder == null) {
            return findFreeLower(hiBorder);
        }

        return findFreeWithBorders(loBorder, hiBorder);
    }

    private UnitIdPart findFreeWithBorders(UnitIdPart loBorder, UnitIdPart hiBorder) {
        Validate.isTrue(loBorder.compareTo(hiBorder) < 0);

        // check if exists space between borders
        // e.g. 1 and 2 is without space
        //      1 and 3 is with space -> take space
        //      1 and 1+1 is without space
        //      1-1 and 1 is without space

        // find number of common particles
        int size = loBorder.countSameParticles(hiBorder);
        // check is space exists in the size
        int lDiffValue = (size < loBorder.getParticlesCount()) ? loBorder.getParticle(size) : 1;
        int hDiffValue = (size < hiBorder.getParticlesCount()) ? hiBorder.getParticle(size) : 0;

        // iterate diff values
        // we have to start with lDiffValue
        // e.g. 1-1 and 2 -> we should assign 1
        // e,g. 1 and 2+1 -> we should assign 2
        for (int v = lDiffValue; v <= hDiffValue; v++) {
            UnitIdPart candidate;
            if (v != 0) {
                candidate = loBorder.getWithLastParticle(size, v);
            } else {
                candidate = loBorder.getWithNumParticles(size);
            }
            if (candidate.compareTo(loBorder) <= 0) {
                continue;
            }
            // check that not too big
            if (candidate.compareTo(hiBorder) >= 0) {
                break;
            }
            // check iff used
            if (!isSealed(candidate)) {
                return candidate;
            }
        }

        // direct free item between lo and hi not found
        // check if exists space -> has to be exists (loBorder+1)+xxx -> find xxx
        if ((hDiffValue - lDiffValue) >= 2) {

            int v = lDiffValue + 1;
            // zero cannot be particle
            if (v != 0) {
                UnitIdPart candidate = loBorder.getWithLastParticle(size, v).addParticle(1);
                return findFreeIncremented(candidate);
            }
        }

        // there is no space between lo and hi
        // e.g. 1, 2
        // e.g. 1+1, 1+2
        // e.g. 1, 1+1
        // e.g. 1-1, 1+1
        // e.g. 1, 1+1+1
        // e.g. 1+1, 1+2+5
        // e.g. 1+1+7+8, 1+2+5+1+5

        // check if loBoarder is final (cannot be incremented)
        // e.g. 1 1+1        
        //      2 2+2+1
        if (loBorder.getParticlesCount() == size) {
            // try to add particle (lDiffValue = 1)
            UnitIdPart candidate = loBorder.addParticle(1);
            if (candidate.compareTo(hiBorder) >= 0) {
                // cannot increment loBoarder -> have to decrement hiBoarder
                // e.g. 1 1+1
                candidate = hiBorder.addParticle(-1);
                return findFreeDecremented(candidate);
            } else {
                // check iff used was done in 
                // previous for cycle because lDiffValue = 1

                // recursive call
                return findFreeWithBorders(candidate, hiBorder);
            }
        }

        // we have value
        // prefix+x+[...] prefix+y[+...] and x!=y
        // -> have to exists prefix+x+z < prefix+y[+...]
        UnitIdPart candidate;
        if (loBorder.getParticlesCount() == (size + 1)) {
            candidate = loBorder.getWithLastParticle(size + 1, 1);
        } else {
            int lastValue = loBorder.getParticle(size + 1);
            candidate = loBorder.getWithLastParticle(size + 1, lastValue).getGreater();
        }
        return findFreeIncremented(candidate);
    }

    private UnitIdPart findFreeIncremented(UnitIdPart candidate) {
        while (isSealed(candidate)) {
            candidate = candidate.getGreater();
        }
        return candidate;
    }

    private UnitIdPart findFreeDecremented(UnitIdPart candidate) {
        while (isSealed(candidate)) {
            candidate = candidate.getLower();
        }
        return candidate;
    }

    boolean isSealed(UnitIdPart candidate) {
        PartSealedUnitId item = units.get(candidate);
        if (item == null) {
            return false;
        } else {
            return item.isFullySealed();
        }
    }

    /**
     * 
     * @param hiBorder
     *            not null
     * @return
     */
    private UnitIdPart findFreeLower(UnitIdPart hiBorder) {
        Validate.notNull(hiBorder);

        // try to find base value hiBoarder-1, ..., 2, 1 
        // or xx+y, xx+(y-1)....xx+(y-n)
        // or xx-y, xx-(y-1)....xx-(y-n)
        UnitIdPart candidate = hiBorder.getLower();
        while (candidate != null) {
            // check if sealed
            if (!isSealed(candidate)) {
                return candidate;
            }
            // increment candidate
            candidate = candidate.getLower();
        }

        // nobase value -> we can try (hiBoarder-1)+...
        candidate = hiBorder.getLower();
        if (candidate != null) {
            // such value has to exists
            candidate = candidate.getWithAdditionalParticle(1);
            while (true) {
                // check if sealed
                if (!isSealed(candidate)) {
                    return candidate;
                }
                // increment candidate
                candidate = candidate.getGreater();
            }
        } else {
            // value cannot be decremented and was not decremented befoure -> hiBoarder is 1
            // -> we have to find free 1-...xx
            candidate = UnitIdPart.getLowest().getWithAdditionalParticle(-1);
            while (true) {
                if (!isSealed(candidate)) {
                    return candidate;
                }
                // decrement candidate
                candidate = candidate.getLower();
            }
        }
    }

    private UnitIdPart findFreeHigher(UnitIdPart loBorder) {
        UnitIdPart candidate;
        if (loBorder != null) {
            candidate = loBorder.getHigherBase();
        } else {
            candidate = UnitIdPart.getLowest();
        }

        return findFreeIncremented(candidate);
    }

    /**
     * Method to print value of this level
     * 
     * @param sb
     */
    void printValue(StringBuilder sb) {
        parent.printValue(sb);
        // print separator for non empty builder
        if (sb.length() > 0) {
            sb.append(levelType.getPrintSeparator());
        }
    }
}
