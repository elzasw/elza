package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.domain.AccessPointItem;
import org.locationtech.jts.geom.Geometry;
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;

import java.util.Objects;

import javax.persistence.EntityManager;

public class ApItemCoordinatesVO extends ApItemVO {

    /**
     * Souřadnice.
     */
    private String value;

    public ApItemCoordinatesVO() {
    }

    public ApItemCoordinatesVO(final AccessPointItem item) {
        super(item);
        value = getCoordinatesValue(item);
    }

    final public String getCoordinatesValue(final AccessPointItem item) {
        ArrDataCoordinates data = (ArrDataCoordinates) item.getData();
        return data == null ? null : GeometryConvertor.convert(data.getValue());
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataCoordinates data = new ArrDataCoordinates();
        Geometry geo = GeometryConvertor.convert(value);
        data.setValue(geo);
        data.setDataType(DataType.COORDINATES.getEntity());
        return data;
    }

    @Override
    public boolean equalsValue(AccessPointItem item) {
        return equalsBase(item) && Objects.equals(value, getCoordinatesValue(item));
    }
}
