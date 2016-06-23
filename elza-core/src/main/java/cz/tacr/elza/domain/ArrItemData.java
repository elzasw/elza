package cz.tacr.elza.domain;

/**
 * Abstraktní datový objekt.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public abstract class ArrItemData implements cz.tacr.elza.api.ArrItemData {

    protected RulItemSpec spec;

    public RulItemSpec getSpec() {
        return spec;
    }

    public void setSpec(final RulItemSpec spec) {
        this.spec = spec;
    }
}
