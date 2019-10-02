package cz.tacr.elza.bulkaction.generator.unitid;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.bulkaction.generator.unitid.PartSealedUnitId.SealType;

public abstract class AssignedUnitId {

    static public enum LevelType {
        /**
         * Standard level without prefix
         */
        DEFAULT(""),
        /**
         * Level with extra slash
         */
        SLASHED("/");

        final private String prefix;

        final private String printSeparator;

        LevelType(String prefix) {
            this.prefix = prefix;
            this.printSeparator = "/" + prefix;
        }

        int getPrefixLength() {
            return prefix.length();
        }

        public String getPrefix() {
            return prefix;
        }

        public String getPrintSeparator() {
            return printSeparator;
        }

    }

    Map<LevelType, SealedLevel> levels = new HashMap<>();

    public SealedLevel getLevel(LevelType levelType) {
        SealedLevel level = levels.get(levelType);
        if (level == null) {
            level = new SealedLevel(this, levelType);
            levels.put(levelType, level);
        }
        return level;
    }

    public boolean isLeaf() {
        for (SealedLevel level : levels.values()) {
            if (level.getUnitCount() > 0) {
                return false;
            }
        }
        return true;
    }

    public PartSealedUnitId find(String value) {
        if (value == null || value.length() == 0) {
            return null;
        }

        // parse prefix
        LevelType levelType = AssignedUnitId.getPrefixPart(value);
        value = value.substring(levelType.getPrefixLength());
        SealedLevel level = getLevel(levelType);

        return find(level, value);
    }

    private PartSealedUnitId find(SealedLevel level, String value) {
        // prepare part
        String partString = AssignedUnitId.getFirstPart(value);
        UnitIdPart part;
        // Parse part
        try {
            part = UnitIdPart.parse(partString);
        } catch (UnitIdException e) {
            return null;
        }

        String remaining = value.substring(partString.length());
        // strip slash in remaining
        if (remaining != null && remaining.length() > 0) {
            remaining = remaining.substring(1);
        }
        return level.find(part, remaining);
    }

    /**
     * 
     * @param value
     * @param seal
     *            Flag if value should be sealed
     * @return
     * @throws UnitIdException
     */
    public PartSealedUnitId addValue(String value, SealType sealType, SealValidator validator) throws UnitIdException {
        Validate.isTrue(value != null);
        Validate.isTrue(value.length() > 0);

        // parse prefix
        LevelType levelType = AssignedUnitId.getPrefixPart(value);
        value = value.substring(levelType.getPrefixLength());
        SealedLevel level = getLevel(levelType);

        // prepare part
        String partString = AssignedUnitId.getFirstPart(value);
        // Parse part
        UnitIdPart part = UnitIdPart.parse(partString);

        String remaining = value.substring(partString.length());
        if (remaining != null && remaining.length() > 0) {
            // strip first slash
            remaining = remaining.substring(1);

            if (remaining.length() == 0) {
                // part without data
                throw new UnitIdException("Incorrect value, no data after last slash, value: " + value);
            }
        } else {
            remaining = null;
        }

        return level.add(part, remaining, sealType, validator);
    }

    abstract public int getDepth();

    /**
     * Return first valid part till next slash
     * 
     * @param unitId
     * @return
     *         if input is "1" return "1"
     *         if input is "/1" return ""
     *         if input is "1/2" return "1"
     *         if input is "1//2" return "1"
     */
    static String getFirstPart(String unitId) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        // append chars till next slash
        while (pos < unitId.length()) {
            char c = unitId.charAt(pos);
            if (c == '/') {
                break;
            }
            result.append(c);
            pos++;
        }
        return result.toString();
    }

    public static LevelType getPrefixPart(String value) {
        // find longest prefix
        LevelType levelType = LevelType.DEFAULT;
        if (value.startsWith(LevelType.SLASHED.getPrefix())) {
            levelType = LevelType.SLASHED;
        }

        return levelType;
    }

    abstract public void printValue(StringBuilder sb);
}
