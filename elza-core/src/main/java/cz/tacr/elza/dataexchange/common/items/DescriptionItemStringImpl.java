package cz.tacr.elza.dataexchange.common.items;

import com.vividsolutions.jts.io.ParseException;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.common.GeometryConvertor;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.schema.v2.DescriptionItemString;

public class DescriptionItemStringImpl extends DescriptionItemString {

    /**
     * Returns description item value.
     *
     * @throws DEImportException When value length exceed max size of the data type.
     */
    public String getV(DataType dataType) {
        if (getV().length() > dataType.getValueMaxSize()) {
            throw new DEImportException("Value exceeded max size, data type:" + dataType + ", maxSize:" + dataType.getValueMaxSize()
                    + ", value:" + getV());
        }
        return getV();
    }

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        switch (dataType) {
            case STRING:
                ArrDataString str = new ArrDataString();
                str.setValue(getV(dataType));
                return new ImportableItemData(str);
            case TEXT:
            case FORMATTED_TEXT:
                ArrDataText txt = new ArrDataText();
                txt.setValue(getV(dataType));
                return new ImportableItemData(txt);
            case COORDINATES:
                ArrDataCoordinates geo = new ArrDataCoordinates();
                try {
                    geo.setValue(GeometryConvertor.convert(getV()));
                } catch (ParseException e) {
                    throw new DEImportException("Failed to convert geo location", e);
                }
                return new ImportableItemData(geo);
            case UNITID:
                ArrDataUnitid id = new ArrDataUnitid();
                id.setValue(getV(dataType));
                return new ImportableItemData(id);
            default:
                throw new DEImportException("Unsupported data type:" + dataType);
        }
    }
}
