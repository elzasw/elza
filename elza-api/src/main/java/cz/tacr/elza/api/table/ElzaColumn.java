package cz.tacr.elza.api.table;

/**
 * Sloupec v tabulce.
 *
 * @author Martin Šlapa
 * @since 21.06.2016
 */
public interface ElzaColumn {

    enum DataType {
        TEXT, INTEGER
    }

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);

    DataType getDataType();

    void setDataType(DataType dataType);

    Integer getWidth();

    void setWidth(Integer width);
}
