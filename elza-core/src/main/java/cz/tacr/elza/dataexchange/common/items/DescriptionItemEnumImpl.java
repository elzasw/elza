package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;

public class DescriptionItemEnumImpl extends DescriptionItemEnum {

	@Override
	public ImportableItemData createData(ImportContext context, DataType dataType) {
		if (dataType != DataType.ENUM) {
			throw new DEImportException("Unsupported data type:" + dataType);
		}
		ArrDataNull data = new ArrDataNull();
		data.setDataType(dataType.getEntity());
		
		return new ImportableItemData(data);
	}
}
