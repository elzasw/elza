package cz.tacr.elza.domain.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Tabulka.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public class ElzaTable {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private List<ElzaRow> rows;

    public List<ElzaRow> getRows() {
        if (rows == null) {
            rows = new ArrayList<>();
        }
        return rows;
    }

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

    public void addRow(final ElzaRow row) {
        getRows().add(row);
    }

    public static String toJsonString(final ElzaTable table) {
        try {
            return objectMapper.writeValueAsString(table);
        } catch (JsonProcessingException e) {
            throw new SystemException("Problém při parsování JSON", e, BaseCode.JSON_PARSE);
        }
    }

    public static ElzaTable fromJsonString(final String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, ElzaTable.class);
        } catch (IOException e) {
            throw new SystemException("Problém při generování JSON", e, BaseCode.JSON_PARSE);
        }
    }

    /**
     * Smaže všechny řádky v tabulce.
     */
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
