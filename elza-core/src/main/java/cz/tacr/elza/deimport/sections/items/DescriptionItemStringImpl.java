package cz.tacr.elza.deimport.sections.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.aps.AccessPointProcessor;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.schema.v2.DescriptionItemString;

public class DescriptionItemStringImpl extends DescriptionItemString {

    @Override
    protected boolean isDataTypeSupported(DataType dataType) {
        return dataType == DataType.STRING || dataType == DataType.TEXT || dataType == DataType.COORDINATES || dataType == DataType.UNITID;
    }

    @Override
    protected ArrData createData(ImportContext context, DataType dataType) {
        switch (dataType) {
            case STRING:
                ArrDataString str = new ArrDataString();
                str.setValue(getV());
                return str;
            case TEXT:
                ArrDataText txt = new ArrDataText();
                txt.setValue(getV());
                return txt;
            case COORDINATES:
                ArrDataCoordinates coords = new ArrDataCoordinates();
                coords.setValue(AccessPointProcessor.convertGeoLocation(getV()));
                return coords;
            case UNITID:
            	ArrDataUnitid unitid = new ArrDataUnitid();
            	unitid.setValue(getV());
            	return unitid;
            default:
                throw new DEImportException("Unsupported data type for string, code:" + dataType.getCode());
        }
    }
}
