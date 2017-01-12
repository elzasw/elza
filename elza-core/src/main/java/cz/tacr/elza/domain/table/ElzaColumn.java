package cz.tacr.elza.domain.table;

import java.util.Objects;

/**
 * Sloupec v tabulce.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public class ElzaColumn {

    private String code;

    private String name;

    private DataType dataType;

    private Integer width;

    public enum DataType {
        TEXT, INTEGER
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(final DataType dataType) {
        this.dataType = dataType;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(final Integer width) {
        this.width = width;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElzaColumn that = (ElzaColumn) o;
        return Objects.equals(code, that.code) &&
                dataType == that.dataType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, dataType);
    }
}
