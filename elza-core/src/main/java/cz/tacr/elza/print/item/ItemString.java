package cz.tacr.elza.print.item;

import org.apache.commons.lang3.StringUtils;

/**
 * Item string
 *
 */
public class ItemString extends AbstractItem {

    private final String value;

    public ItemString(final String value) {
        this.value = value;
    }

    @Override
    public String getSerializedValue() {
        return StringUtils.trim(value);
    }

    @Override
    protected String getValue() {
        return value;
    }
}
