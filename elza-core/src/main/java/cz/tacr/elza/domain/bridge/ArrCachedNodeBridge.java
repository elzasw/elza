package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;
import static cz.tacr.elza.domain.ArrDescItem.NORM_FROM;
import static cz.tacr.elza.domain.ArrDescItem.NORM_TO;
import static cz.tacr.elza.domain.ArrDescItem.REL_AP_ID;

import java.math.BigDecimal;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;

public class ArrCachedNodeBridge implements TypeBridge<ArrCachedNode> {

    // TODO převést na použití Bean
	private static NodeCacheService nodeCacheService;

	public static void init(NodeCacheService nodeCacheService) {
		ArrCachedNodeBridge.nodeCacheService = nodeCacheService;
	}

	@Override
	public void write(DocumentElement document, ArrCachedNode arrCachedNode, TypeBridgeWriteContext context) {

    	RestoredNode cachedNode = nodeCacheService.deserialize(arrCachedNode);
    	nodeCacheService.reloadCachedNodes(Collections.singletonList(cachedNode));

    	// TODO: do not index deleted levels

    	document.addValue(FIELD_FUND_ID, cachedNode.getFundId());
    	if (cachedNode.getDescItems() != null) {
            for (ArrDescItem item : cachedNode.getDescItems()) {
            	// skip item with no data
            	if (item.getData() == null) {
            		continue;
            	}
            	// TODO: consider indexing with specifications
            	String fullTextValue = item.getFulltextValue();
            	if (StringUtils.isNotBlank(fullTextValue)) {
            		document.addValue(FULLTEXT_ATT, fullTextValue);
            	}
            	// item type & ipem spec codes
            	String itemTypeCodeLowerCase = item.getItemType().getCode().toLowerCase();
            	String itemSpecCodeLowerCase = null;
            	if (item.getItemSpec() != null) {
            		itemSpecCodeLowerCase = item.getItemSpec().getCode().toLowerCase();
            	}
            	// get dataType
            	DataType dataType = DataType.fromId(item.getData().getDataTypeId());
            	if (dataType != null) {
            		switch (dataType) {
            		case INT:
            			document.addValue(itemTypeCodeLowerCase, item.getValueInt());
            			break;
            		case DECIMAL:
            			BigDecimal decimalValue = new BigDecimal(item.getValueDouble());
            			document.addValue(itemTypeCodeLowerCase, decimalValue);
						if (itemSpecCodeLowerCase != null) {
							document.addValue(itemTypeCodeLowerCase + "_" + itemSpecCodeLowerCase, decimalValue);
						}
            			break;
            		case ENUM:
						document.addValue(itemTypeCodeLowerCase, itemSpecCodeLowerCase);
						break;
            		case RECORD_REF:
						document.addValue(REL_AP_ID, ((ArrDataRecordRef) item.getData()).getRecordId());
            		case STRUCTURED:
					case STRING:
					case TEXT:
						document.addValue(itemTypeCodeLowerCase, fullTextValue);
						if (itemSpecCodeLowerCase != null) {
							document.addValue(itemTypeCodeLowerCase + "_" + itemSpecCodeLowerCase, fullTextValue);
						}
						break;
					case UNITDATE:
						ArrDataUnitdate unitDate = (ArrDataUnitdate) item.getData();
						document.addValue(itemTypeCodeLowerCase + "_" + NORM_FROM, unitDate.getNormalizedFrom());
						document.addValue(itemTypeCodeLowerCase + "_" + NORM_TO, unitDate.getNormalizedTo());
						break;
            		}
            	}
            }
    	}
	}
}
