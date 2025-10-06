package cz.tacr.elza.domain.bridge;

import java.util.Collection;

import cz.tacr.elza.core.data.DataType;

public interface IndexConfigReader {

	Collection<String> getPartTypeCodes();

	Collection<String> getItemTypeCodes();

	Collection<String> getItemSpecCodesByTypeCode(String itemTypeCode);

	DataType getDataTypeByItemTypeCode(String itemTypeCode);
}
