package cz.tacr.elza.domain.table;

import java.util.Objects;

/**
 * Implementace {@link cz.tacr.elza.api.table.ElzaColumn}
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public class ElzaColumn implements cz.tacr.elza.api.table.ElzaColumn {

    private String code;

    private String name;

    private DataType dataType;

    private Integer width;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(final DataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public Integer getWidth() {
        return width;
    }

    @Override
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
