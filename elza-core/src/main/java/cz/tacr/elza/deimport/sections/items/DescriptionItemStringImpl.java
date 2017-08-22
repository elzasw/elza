package cz.tacr.elza.deimport.sections.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.aps.AccessPointProcessor;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.schema.v2.DescriptionItemString;

public class DescriptionItemStringImpl extends DescriptionItemString {

    @Override
    protected boolean isDataTypeSupported(DataType dataType) {
        return dataType == DataType.STRING || dataType == DataType.TEXT || dataType == DataType.COORDINATES;
    }

    @Override
    protected ArrData createData(ImportContext context, RuleSystemItemType itemType) {
        switch (itemType.getDataType()) {
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
            default:
                throw new DEImportException("Unsupported item type for string, code:" + itemType.getCode());
        }
    }
}
