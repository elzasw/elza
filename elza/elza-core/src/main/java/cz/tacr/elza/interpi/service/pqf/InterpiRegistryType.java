package cz.tacr.elza.interpi.service.pqf;

/**
 * Typy entit v INTERPI.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 6. 12. 2016
 */
public enum InterpiRegistryType {

    PERSON("'o'", true),
    DYNASTY("'r'", true),
    PARTY_GROUP("'k'", true),
    EVENT("'u'", true),
    GEO("'g'", false),
    ARTWORK("'d'", false),
    TERM("'p'", false);

    private String value;
    private boolean isParty;

    private InterpiRegistryType(final String value, final boolean isParty) {
        this.value = value;
        this.isParty = isParty;
    }

    public String getValue() {
        return value;
    }

    public boolean isParty() {
        return isParty;
    }
}
