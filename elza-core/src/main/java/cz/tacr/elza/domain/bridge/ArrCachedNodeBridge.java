package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;
import static cz.tacr.elza.domain.ArrDescItem.NORM_FROM;
import static cz.tacr.elza.domain.ArrDescItem.NORM_TO;
import static cz.tacr.elza.domain.ArrDescItem.REL_AP_ID;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityError;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrNodeConformityMissing;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;

public class ArrCachedNodeBridge implements TypeBridge<ArrCachedNode> {

    // TODO převést na použití Bean
	private static NodeCacheService nodeCacheService;
	private static RuleService ruleService;
	private static ArrangementInternalService arrangementInternalService;

	public static void init(final NodeCacheService nodeCacheService, 
			final RuleService ruleService,
			final ArrangementInternalService arrangementInternalService) {
		ArrCachedNodeBridge.nodeCacheService = nodeCacheService;
		ArrCachedNodeBridge.ruleService = ruleService;
		ArrCachedNodeBridge.arrangementInternalService = arrangementInternalService;
	}

	@Override
	public void write(DocumentElement document, ArrCachedNode arrCachedNode, TypeBridgeWriteContext context) {

    	RestoredNode cachedNode = nodeCacheService.deserialize(arrCachedNode);
    	nodeCacheService.reloadCachedNodes(Collections.singletonList(cachedNode));
    	
    	ArrFundVersion fundVersion = arrangementInternalService.getOpenVersionByFundId(cachedNode.getFundId());

    	// read node_conformity for last opened version
    	Map<Integer, ArrNodeConformityExt> errors = ruleService.getNodeConformityInfoForNodes(Collections.singletonList(cachedNode.getNodeId()), fundVersion);
    	ArrNodeConformityExt conformityErrors = errors.get(cachedNode.getNodeId());
    	
    	// TODO: do not index deleted levels

    	document.addValue(FIELD_FUND_ID, cachedNode.getFundId());
    	document.addValue(ArrCachedNodeBinder.UUID, cachedNode.getUuid());
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
            	String itemTypeAndSpecCodeLowerCase = null;
            	if (item.getItemSpec() != null) {
            		itemSpecCodeLowerCase = item.getItemSpec().getCode().toLowerCase();
            		itemTypeAndSpecCodeLowerCase = itemTypeCodeLowerCase + "_" + itemSpecCodeLowerCase;
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
						if (itemTypeAndSpecCodeLowerCase != null) {
							document.addValue(itemTypeAndSpecCodeLowerCase, decimalValue);
						}
            			break;
            		case ENUM:
						document.addValue(itemTypeCodeLowerCase, itemSpecCodeLowerCase);
						break;
            		case RECORD_REF:
            			Integer recordId = ((ArrDataRecordRef) item.getData()).getRecordId();
						document.addValue(REL_AP_ID, recordId);
            			document.addValue(itemTypeCodeLowerCase, recordId);
						if (itemTypeAndSpecCodeLowerCase != null) {
							document.addValue(itemTypeAndSpecCodeLowerCase, recordId);
						}
						break;
            		case STRUCTURED:
					case STRING:
					case TEXT:
						document.addValue(itemTypeCodeLowerCase, fullTextValue);
						if (itemTypeAndSpecCodeLowerCase != null) {
							document.addValue(itemTypeAndSpecCodeLowerCase, fullTextValue);
						}
						break;
					case UNITDATE:
						ArrDataUnitdate unitDate = (ArrDataUnitdate) item.getData();
						document.addValue(itemTypeCodeLowerCase + "_" + NORM_FROM, unitDate.getNormalizedFrom());
						document.addValue(itemTypeCodeLowerCase + "_" + NORM_TO, unitDate.getNormalizedTo());
						if (itemTypeAndSpecCodeLowerCase != null) {
							document.addValue(itemTypeAndSpecCodeLowerCase + "_" + NORM_FROM, unitDate.getNormalizedFrom());
							document.addValue(itemTypeAndSpecCodeLowerCase + "_" + NORM_TO, unitDate.getNormalizedTo());
						}
						break;
            		}
            	}
            }
    	}

    	// index node conformity error & missing
        if (conformityErrors != null && !conformityErrors.getState().equals(ArrNodeConformity.State.OK)) {
        	if (conformityErrors.getErrorList() != null) {
        		for (ArrNodeConformityError error : conformityErrors.getErrorList()) {
        			if (StringUtils.isNotBlank(error.getDescription())) {
        				document.addValue(ArrCachedNodeBinder.CONFORMITY_ERROR, error.getDescription());
        			}
        		}
        	}
        	if (conformityErrors.getMissingList() != null) {
				for (ArrNodeConformityMissing missing : conformityErrors.getMissingList()) {
					if (StringUtils.isNotBlank(missing.getDescription())) {
						document.addValue(ArrCachedNodeBinder.CONFORMITY_MISSING, missing.getDescription());
					}
				}
			}
        }
	}
}
