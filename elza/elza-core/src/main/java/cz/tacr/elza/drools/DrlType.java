package cz.tacr.elza.drools;

public enum DrlType {

    /**
     * Dostupné prvky popisu pro jednotlivé party a třídy entit.
     */
    AVAILABLE_ITEMS,

    /**
     * Validace entity jako celku pro různé třídy.
     */
    VALIDATION;

    public String value() {
        return name();
    }

    public static DrlType fromValue(String v) {
        return valueOf(v);
    }
}
