package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrDescItem.FIELD_ITEM_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_NODE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.SPECIFICATION_ATT;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_DESC_ITEM_TYPE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_CREATE_CHANGE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;
import static cz.tacr.elza.domain.ArrDescItem.INTGER_ATT;

import java.util.Map;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.domain.ArrDescItem;

public class ArrDescItemBridge implements TypeBridge<ArrDescItem> {

	private final static Logger log = LoggerFactory.getLogger(ArrDescItemBridge.class);

    private Map<String, IndexFieldReference<String>> fields;

    public ArrDescItemBridge(Map<String, IndexFieldReference<String>> fields) {
        this.fields = fields;
        log.debug("Creating ArrDescItemBridge");
    }

    @Override
	public void write(DocumentElement document, ArrDescItem arrDescItem, TypeBridgeWriteContext context) {
    	document.addValue(FIELD_ITEM_ID, arrDescItem.getItemId().toString());
    	document.addValue(FIELD_NODE_ID, arrDescItem.getNodeId().toString());
    	document.addValue(FIELD_FUND_ID, arrDescItem.getFundId().toString());

    	if (arrDescItem.getItemSpecId() != null) {
    		document.addValue(SPECIFICATION_ATT, arrDescItem.getItemSpecId().toString());
    	}
    	document.addValue(FIELD_DESC_ITEM_TYPE_ID, arrDescItem.getDescItemTypeId().toString());
    	document.addValue(FIELD_CREATE_CHANGE_ID, arrDescItem.getCreateChangeId().toString());

    	if (arrDescItem.getFulltextValue() != null) {
    		document.addValue(FULLTEXT_ATT, arrDescItem.getFulltextValue());
    	}
    	if (arrDescItem.getValueInt() != null) {
    		document.addValue(INTGER_ATT, arrDescItem.getValueInt());
    	}
    }
}
