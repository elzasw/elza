package cz.tacr.elza.controller.vo.nodes.descitems;

import javax.persistence.EntityManager;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

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
        // convert value
        WKTReader reader = new WKTReader();
        try {
            data.setValue(reader.read(value));
        } catch (ParseException e) {
            throw new BusinessException("Failed to parse value: " + value, e, BaseCode.PROPERTY_IS_INVALID);
        }
        data.setDataType(DataType.COORDINATES.getEntity());
        return null;
    }
}