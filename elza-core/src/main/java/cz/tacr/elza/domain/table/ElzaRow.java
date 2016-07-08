package cz.tacr.elza.domain.table;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementace {@link cz.tacr.elza.api.table.ElzaRow}
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public class ElzaRow implements cz.tacr.elza.api.table.ElzaRow {

    private Map<String, String> values;

    public ElzaRow(Map.Entry<String, String>... cells) {
        values = new HashMap<>();
        for (Map.Entry<String, String> cell : cells) {
            values.put(cell.getKey(), cell.getValue());
        }
    }

    public ElzaRow() {
    }

    @Override
    public Map<String, String> getValues() {
        return values;
    }

    @Override
    public void setValues(final Map<String, String> values) {
        this.values = values;
    }

    @Override
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
}
