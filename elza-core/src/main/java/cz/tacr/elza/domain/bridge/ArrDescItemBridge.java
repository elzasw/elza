package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrDescItem.FIELD_ITEM_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_NODE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.SPECIFICATION_ATT;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_DESC_ITEM_TYPE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_CREATE_CHANGE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_DELETE_CHANGE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;
import static cz.tacr.elza.domain.ArrDescItem.INTGER_ATT;
import static cz.tacr.elza.domain.ArrDescItem.NORMALIZED_FROM_ATT;
import static cz.tacr.elza.domain.ArrDescItem.NORMALIZED_TO_ATT;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.domain.ArrDescItem;

public class ArrDescItemBridge implements TypeBridge<ArrDescItem> {

	private final static Logger log = LoggerFactory.getLogger(ArrDescItemBridge.class);

    public ArrDescItemBridge() {
        log.debug("Creating ArrDescItemBridge");
    }

    @Override
	public void write(DocumentElement document, ArrDescItem arrDescItem, TypeBridgeWriteContext context) {
    	document.addValue(FIELD_ITEM_ID, arrDescItem.getItemId());
    	document.addValue(FIELD_NODE_ID, arrDescItem.getNodeId());
    	document.addValue(FIELD_FUND_ID, arrDescItem.getFundId());

    	document.addValue(SPECIFICATION_ATT, arrDescItem.getItemSpecId());
    	document.addValue(FIELD_DESC_ITEM_TYPE_ID, arrDescItem.getDescItemTypeId());
    	document.addValue(FIELD_CREATE_CHANGE_ID, arrDescItem.getCreateChangeId());
    	document.addValue(FIELD_DELETE_CHANGE_ID, arrDescItem.getDeleteChangeId());

    	if (arrDescItem.getFulltextValue() != null) {
    		document.addValue(FULLTEXT_ATT, arrDescItem.getFulltextValue());
    	}
		document.addValue(INTGER_ATT, arrDescItem.getValueInt());
		document.addValue(NORMALIZED_FROM_ATT, arrDescItem.getNormalizedFrom());
		document.addValue(NORMALIZED_TO_ATT, arrDescItem.getNormalizedTo());
    }
}
