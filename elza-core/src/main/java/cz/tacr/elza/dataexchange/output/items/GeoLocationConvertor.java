package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.GeometryConvertor;
import cz.tacr.elza.dataexchange.common.items.DescriptionItemStringImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;

public class GeoLocationConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemStringImpl convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataCoordinates.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataCoordinates gl = (ArrDataCoordinates) data;
        DescriptionItemStringImpl item = new DescriptionItemStringImpl();
        item.setV(GeometryConvertor.convert(gl.getValue()));
        return item;
    }
}
