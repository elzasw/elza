package cz.tacr.elza.domain.bridge;

import java.util.List;

public interface IndexConfigReader {
	
	List<String> getPartTypeCodes();
	
	List<String> getItemTypeCodes();
	
	List<String> getItemSpecCodesByTypeCode(String itemTypeCode);
}
