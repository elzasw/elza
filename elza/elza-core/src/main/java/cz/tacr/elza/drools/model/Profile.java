package cz.tacr.elza.drools.model;

public class Profile {

    /**
     * Kód profilu.
     */
    private final String code;

    public Profile(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
