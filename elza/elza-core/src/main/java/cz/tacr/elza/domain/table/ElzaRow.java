package cz.tacr.elza.domain.table;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Řádek v tabulce.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public class ElzaRow {

    private Map<String, String> values;

    @SafeVarargs
    public ElzaRow(final Map.Entry<String, String>... cells) {
        values = new HashMap<>();
        for (Map.Entry<String, String> cell : cells) {
            values.put(cell.getKey(), cell.getValue());
        }
    }

    public ElzaRow() {
    }

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(final Map<String, String> values) {
        this.values = values;
    }

    public void setValue(final String key, final String value) {
        if (values == null) {
            values = new HashMap<>();
        }

        if (StringUtils.isEmpty(value)) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElzaRow row = (ElzaRow) o;
        return Objects.equals(values, row.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    public String getValue(String columnName) {
        if (values == null) {
            return null;
        }
        return values.get(columnName);
    }
}
