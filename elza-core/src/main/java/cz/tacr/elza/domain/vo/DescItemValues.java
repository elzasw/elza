package cz.tacr.elza.domain.vo;

import java.util.Collection;

/**
 * Obalovací třída pro více hodnot atributů (kvůli opakovatelným).
 */
public class DescItemValues {

    private Collection<DescItemValue> values;

    public Collection<DescItemValue> getValues() {
        return values;
    }

    public void setValues(Collection<DescItemValue> values) {
        this.values = values;
    }
}
