package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.table.ElzaTable;

import java.util.Objects;

import javax.persistence.EntityManager;

public class ApItemJsonTableVO extends ApItemVO {

    /**
     * celé číslo
     */
    private ElzaTable value;

    public ApItemJsonTableVO() {
    }

    public ApItemJsonTableVO(final ApItem item) {
        super(item);
        value = getElzaTableValue(item);
    }

    final public ElzaTable getElzaTableValue(final ApItem item) {
        ArrDataJsonTable data = (ArrDataJsonTable) item.getData();
        return data == null ? null : data.getValue();
    }

    public ElzaTable getValue() {
        return value;
    }

    public void setValue(final ElzaTable value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataJsonTable data = new ArrDataJsonTable();
        data.setValue(value);
        data.setDataType(DataType.JSON_TABLE.getEntity());
        return data;
    }

    @Override
    public boolean equalsValue(ApItem item) {
        return equalsBase(item) && Objects.equals(value, getElzaTableValue(item));
    }

}
