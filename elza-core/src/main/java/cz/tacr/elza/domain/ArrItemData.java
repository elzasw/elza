package cz.tacr.elza.domain;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.tacr.elza.service.cache.NodeCacheSerializable;

/**
 * Abstraktní datový objekt.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ArrItemData implements NodeCacheSerializable {

    protected RulItemSpec spec;

    public RulItemSpec getSpec() {
        return spec;
    }

    public void setSpec(final RulItemSpec spec) {
        this.spec = spec;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrItemData itemData = (ArrItemData) o;
        return Objects.equals(spec, itemData.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec);
    }
}
