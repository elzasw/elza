package cz.tacr.elza.controller.vo.nodes.descitems;

import jakarta.persistence.EntityManager;

import org.locationtech.jts.geom.Geometry;

import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * VO hodnoty atributu - coordinates.
 *
 * @since 8.1.2016
 */
public class ArrItemCoordinatesVO extends ArrItemVO {

    /**
     * souřadnice
     */
    private String value;

    public ArrItemCoordinatesVO() {
    	
    }

    public ArrItemCoordinatesVO(ArrItem item, final String value) {
        super(item);
        this.value = value;
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
        return null;
    }

    public static ArrItemCoordinatesVO newInstance(ArrItem item) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        String value = null;
        if (data != null) {
            if (!(data instanceof ArrDataCoordinates)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataCoordinates coordinates = (ArrDataCoordinates) data;
            value = coordinates.getFulltextValue();
        }
        ArrItemCoordinatesVO vo = new ArrItemCoordinatesVO(item, value);
        return vo;
    }
}
