package cz.tacr.elza.drools.model;

public enum PartType {

    PT_BODY,
    PT_CRE,
    PT_EVENT,
    PT_EXT,
    PT_IDENT,
    PT_NAME,
    PT_REL;

    public String value() {
        return name();
    }

    public static PartType fromValue(String v) {
        return valueOf(v);
    }
}
