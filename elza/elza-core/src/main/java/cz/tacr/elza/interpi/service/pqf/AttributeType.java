package cz.tacr.elza.interpi.service.pqf;


/**
 * Typy atributů.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 11. 2016
 */
public enum AttributeType {
    /** Hledání v preferovaném jméně rejstříku. */
    PREFFERED_NAME(" @attr 1=2054 "),

    /** Hledání ve všech jménech rejstříku. */
    ALL_NAMES(" @attr 1=2055 "),

    /** Hledání určitých typů rejstříků. */
    TYPE(" @attr 1=2051 "),

    /** Pravostranný like na hledané výrazy ve jménech. */
    EXTEND(" @attr 5=1 "),

    /** Id záznamu. */
    ID(null);

    private String att;

    private AttributeType(final String att) {
        this.att = att;
    }

    public String getAtt() {
        return att;
    }
}
