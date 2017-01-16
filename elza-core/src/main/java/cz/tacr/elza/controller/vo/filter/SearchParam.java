package cz.tacr.elza.controller.vo.filter;

import org.springframework.util.Assert;

/**
 * Abstraktní předek pro vyhledávací paramtery.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 16. 1. 2017
 */
public abstract class SearchParam {

    /** Typ potomka. */
    private SearchParamType type;

    /** Hledaná hodnota. */
    private String value;

    public SearchParam(final SearchParamType type, final String value) {
        Assert.notNull(type);
        Assert.notNull(value);

        this.type = type;
        this.value = value;
    }

    public SearchParamType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }
}
