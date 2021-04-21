package cz.tacr.elza.dataexchange.common.items;

import org.locationtech.jts.geom.Geometry;

import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.schema.v2.DescriptionItemBinary;

public class DescriptionItemBinaryImpl extends DescriptionItemBinary {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.COORDINATES) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }

        ArrDataCoordinates data = new ArrDataCoordinates();
        Geometry value = GeometryConvertor.convertWkb(getD());
        data.setValue(value);
        data.setDataType(dataType.getEntity());

        return new ImportableItemData(data);
    }

}
