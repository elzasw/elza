package cz.tacr.elza.domain.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementace {@link cz.tacr.elza.api.table.ElzaTable}
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public class ElzaTable implements cz.tacr.elza.api.table.ElzaTable<ElzaRow> {


    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(ElzaTable.class);

    private List<ElzaRow> rows;

    @Override
    public List<ElzaRow> getRows() {
        if (rows == null) {
            rows = new ArrayList<>();
        }
        return rows;
    }

    @Override
    public void setRows(final List<ElzaRow> rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        if (rows != null) {
            Integer size = rows.size();
            String res = "Table: " + size + " rows";
            if (size > 0 && rows.get(0).getValues() != null) {
                res += ", " + rows.get(0).getValues().size() + " columns";
            }
            return res;
        } else {
            return "Table: Empty";
        }
    }

    @Override
    public void addRow(final ElzaRow row) {
        getRows().add(row);
    }

    public static String toJsonString(final ElzaTable table) {
        try {
            return objectMapper.writeValueAsString(table);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Problém při parsování JSON", e);
        }
    }

    public static ElzaTable fromJsonString(final String value) {
        try {
            return objectMapper.readValue(value, ElzaTable.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Problém při generování JSON", e);
        }
    }

    @Override
    public void clear() {
        this.rows = null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElzaTable table = (ElzaTable) o;
        return Objects.equals(rows, table.rows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rows);
    }
}
