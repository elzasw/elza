package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class GeoLocationConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemString convert(ArrData data, ObjectFactory objectFactory) {
        Validate.isTrue(data.getClass() == ArrDataCoordinates.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataCoordinates gl = (ArrDataCoordinates) data;
        DescriptionItemString item = objectFactory.createDescriptionItemString();
        item.setV(GeometryConvertor.convert(gl.getValue()));
        return item;
    }
}
