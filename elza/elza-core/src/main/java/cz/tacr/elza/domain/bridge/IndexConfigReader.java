package cz.tacr.elza.domain.bridge;

import java.util.List;

import cz.tacr.elza.core.data.DataType;

public interface IndexConfigReader {

	List<String> getPartTypeCodes();

	List<String> getItemTypeCodes();

	List<String> getItemSpecCodesByTypeCode(String itemTypeCode);

	DataType getDataTypeByItemTypeCode(String itemTypeCode);
}
