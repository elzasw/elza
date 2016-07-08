package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstraktní datový objekt.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ArrItemData implements cz.tacr.elza.api.ArrItemData {

    protected RulItemSpec spec;

    public RulItemSpec getSpec() {
        return spec;
    }

    public void setSpec(final RulItemSpec spec) {
        this.spec = spec;
    }
}
