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
@Deprecated
public abstract class ArrItemData implements NodeCacheSerializable {

    // atribut pouze pro pomocné uložení hodnoty v JSON
    protected RulItemSpec spec;

    // atribut pouze pro pomocné uložení hodnoty v JSON
    protected Boolean undefined;

    public RulItemSpec getSpec() {
        return spec;
    }

    public void setSpec(final RulItemSpec spec) {
        this.spec = spec;
    }

    public Boolean getUndefined() {
        return undefined;
    }

    public void setUndefined(final Boolean undefined) {
        this.undefined = undefined;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
		// Tento kod napsal nekdo bez schopnosti premyslet nebo po tezke noci.
		// Tato metoda equals musi pryc.
        ArrItemData that = (ArrItemData) o;
        return Objects.equals(spec, that.spec) &&
                Objects.equals(undefined, that.undefined);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec, undefined);
    }
}
