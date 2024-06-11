package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.service.cache.NodeCacheService;

public class ArrCachedNodeBridge implements TypeBridge<ArrCachedNode> {

    // TODO převést na použití Bean
	private static NodeCacheService nodeCacheService;

	public static void init(NodeCacheService nodeCacheService) {
		ArrCachedNodeBridge.nodeCacheService = nodeCacheService;
	}

	@Override
	public void write(DocumentElement document, ArrCachedNode arrCachedNode, TypeBridgeWriteContext context) {

    	var cachedNode = nodeCacheService.deserialize(arrCachedNode);
    	nodeCacheService.reloadCachedNodes(Collections.singletonList(cachedNode));

    	document.addValue(FIELD_FUND_ID, cachedNode.getFundId());
    	if (cachedNode.getDescItems() != null) {
            for (ArrDescItem item : cachedNode.getDescItems()) {
            	String fullTextValue = item.getFulltextValue();
            	if (StringUtils.isNotEmpty(fullTextValue)) {
            		document.addValue(FULLTEXT_ATT, fullTextValue);
            	}
            }
    	}
	}
}
