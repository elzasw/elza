package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.domain.ArrDescItem.FIELD_ITEM_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_NODE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_FUND_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_ITEM_SPEC_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_DESC_ITEM_TYPE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_CREATE_CHANGE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FIELD_DELETE_CHANGE_ID;
import static cz.tacr.elza.domain.ArrDescItem.FULLTEXT_ATT;
import static cz.tacr.elza.domain.ArrDescItem.INTEGER_ATT;
import static cz.tacr.elza.domain.ArrDescItem.DECIMAL_ATT;
import static cz.tacr.elza.domain.ArrDescItem.NORM_FROM;
import static cz.tacr.elza.domain.ArrDescItem.NORM_TO;
import static cz.tacr.elza.domain.ArrDescItem.REL_AP_ID;

import static cz.tacr.elza.common.string.Normalizer.normalizeLineEnds;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.Hibernate;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
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

    	document.addValue(FIELD_ITEM_SPEC_ID, arrDescItem.getItemSpecId());
    	document.addValue(FIELD_DESC_ITEM_TYPE_ID, arrDescItem.getDescItemTypeId());
    	document.addValue(FIELD_CREATE_CHANGE_ID, arrDescItem.getCreateChangeId());
    	document.addValue(FIELD_DELETE_CHANGE_ID, arrDescItem.getDeleteChangeId());

    	String fullText = arrDescItem.getFulltextValue();
    	if (StringUtils.isNotBlank(fullText)) {
    		document.addValue(FULLTEXT_ATT, normalizeLineEnds(fullText));
    	}
		document.addValue(INTEGER_ATT, arrDescItem.getValueInt());
		document.addValue(DECIMAL_ATT, arrDescItem.getValueDouble());
		document.addValue(NORM_FROM, arrDescItem.getNormalizedFrom());
		document.addValue(NORM_TO, arrDescItem.getNormalizedTo());

		if (arrDescItem.getData() != null) {
			ArrData unproxiedData = (ArrData) Hibernate.unproxy(arrDescItem.getData());
			if (unproxiedData instanceof ArrDataRecordRef) {
				Integer recordId = ((ArrDataRecordRef) unproxiedData).getRecordId();
				document.addValue(REL_AP_ID, recordId);
			}
		}
    }
}
