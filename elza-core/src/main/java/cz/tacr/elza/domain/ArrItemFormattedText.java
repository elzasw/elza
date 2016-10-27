package cz.tacr.elza.domain;

import cz.tacr.elza.domain.interfaces.IArrItemStringValue;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrItemFormattedText extends ArrItemData implements cz.tacr.elza.api.ArrItemFormattedText, IArrItemStringValue {

    private String value;

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
