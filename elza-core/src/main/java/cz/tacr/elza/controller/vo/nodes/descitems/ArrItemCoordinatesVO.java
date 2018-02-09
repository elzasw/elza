package cz.tacr.elza.controller.vo.nodes.descitems;

import javax.persistence.EntityManager;

import com.vividsolutions.jts.geom.Geometry;

import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;

/**
 * VO hodnoty atributu - coordinates.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemCoordinatesVO extends ArrItemVO {

    /**
     * souřadnice
     */
    private String value;

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
        return null;
    }
}