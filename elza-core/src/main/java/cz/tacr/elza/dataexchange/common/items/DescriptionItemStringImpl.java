package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.schema.v2.DescriptionItemString;

public class DescriptionItemStringImpl extends DescriptionItemString {

	/**
	 * Returns description item value.
	 *
	 * @throws DEImportException
	 *             When value length exceed max size of the data type.
	 */
	public String getV(DataType dataType) {

		return getV();
	}

	@Override
	public ImportableItemData createData(ImportContext context, DataType dataType) {
		String value = getV();
		if (value.length() > dataType.getValueMaxSize()) {
			throw new DEImportException("Value exceeded max size, data type:" + dataType + ", maxSize:"
					+ dataType.getValueMaxSize() + ", value:" + getV());
		}
		ArrData data = createStringData(dataType, value);
		data.setDataType(dataType.getEntity());

		return new ImportableItemData(data);
	}

	private static ArrData createStringData(DataType dataType, String value) {
		switch (dataType) {
		case STRING:
			ArrDataString str = new ArrDataString();
			str.setValue(value);
			return str;
		case TEXT:
		case FORMATTED_TEXT:
			ArrDataText txt = new ArrDataText();
			txt.setValue(value);
			return txt;
		case COORDINATES:
			ArrDataCoordinates geo = new ArrDataCoordinates();
			geo.setValue(GeometryConvertor.convert(value));
			return geo;
		case UNITID:
			ArrDataUnitid id = new ArrDataUnitid();
            id.setUnitId(value);
			return id;
		default:
			throw new DEImportException("Unsupported data type:" + dataType);
		}
	}
}
