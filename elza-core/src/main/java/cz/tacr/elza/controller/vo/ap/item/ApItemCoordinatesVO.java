package cz.tacr.elza.controller.vo.ap.item;

import org.locationtech.jts.geom.Geometry;
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;

import javax.persistence.EntityManager;

/**
 * @since 18.07.2018
 */
public class ApItemCoordinatesVO extends ApItemVO {

    /**
     * Souřadnice.
     */
    private String value;

    public ApItemCoordinatesVO() {
    }

    public ApItemCoordinatesVO(final ApItem item) {
        super(item);
        ArrDataCoordinates data = (ArrDataCoordinates) item.getData();
        value = data == null ? null : GeometryConvertor.convert(data.getValue());
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
}
