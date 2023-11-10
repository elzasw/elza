package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.table.ElzaTable;

import java.util.Objects;

import jakarta.persistence.EntityManager;

public class ApItemJsonTableVO extends ApItemVO {

    /**
     * celé číslo
     */
    private ElzaTable value;

    public ApItemJsonTableVO() {
    }

    public ApItemJsonTableVO(final AccessPointItem item) {
        super(item);
        value = getElzaTableValue(item);
    }

    final public ElzaTable getElzaTableValue(final AccessPointItem item) {
        ArrDataJsonTable data = HibernateUtils.unproxy(item.getData());
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
    public boolean equalsValue(AccessPointItem item) {
        return equalsBase(item) && Objects.equals(value, getElzaTableValue(item));
    }

}
