package cz.tacr.elza.controller.vo.filter;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.util.Assert;

/**
 * Abstraktní předek pro vyhledávací paramtery.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 16. 1. 2017
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include= JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class SearchParam {

    /** Typ potomka. */
    private SearchParamType type;

    /** Hledaná hodnota. */
    private String value;

    public SearchParam() {
    }

    public SearchParam(final SearchParamType type, final String value) {
        Assert.notNull(type, "Typ musí být vyplněn");
        Assert.notNull(value, "Hodnota musí být vyplněna");

        this.type = type;
        this.value = value;
    }

    public SearchParamType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public void setType(final SearchParamType type) {
        this.type = type;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
