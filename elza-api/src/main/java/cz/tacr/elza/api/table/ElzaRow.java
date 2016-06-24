package cz.tacr.elza.api.table;

import java.util.Map;

/**
 * Řádek v tabulce.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public interface ElzaRow {
    Map<String, String> getValues();

    void setValues(Map<String, String> values);

    void setValue(final String key, final String value);
}
