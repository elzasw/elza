package cz.tacr.elza.bulkaction.generator;

import java.util.Arrays;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.service.ArrangementCacheService;
import cz.tacr.elza.service.RuleService;

@Component
@Scope("prototype")
public class MoveDescItem extends BulkActionDFS {

	MoveDescItemConfig config;

	@Autowired
	ArrangementCacheService arrangementCacheService;

	@Autowired
	RuleService ruleService;

	/**
	 * Source item type
	 */
	private RulItemType srcItemType;

	private RulItemType trgItemType;

	private RulItemSpec trgItemSpec;

	public MoveDescItem(MoveDescItemConfig moveDescItemConfig) {
		this.config = moveDescItemConfig;
	}

	@Override
	protected void init(ArrBulkActionRun bulkActionRun) {
		super.init(bulkActionRun);

		// prepare item type
		RuleSystemItemType srcItemType = staticDataProvider.getItemTypeByCode(config.getSource().getItemType());
		Validate.notNull(srcItemType);

		// check if supported source data type
		if (srcItemType.getDataType() != DataType.STRING) {
			throw new SystemException(
			        "Hromadná akce " + getName() + " je nakonfigurována pro nepodporovaný datový typ:",
			        BaseCode.SYSTEM_ERROR).set("itemTypeCode", srcItemType.getCode());
		}

		this.srcItemType = srcItemType.getEntity();

		// prepare target type
		RuleSystemItemType trgItemType = staticDataProvider.getItemTypeByCode(config.getTarget().getItemType());
		Validate.notNull(trgItemType);
		if (trgItemType.getDataType() != DataType.STRING) {
			throw new SystemException(
			        "Hromadná akce " + getName() + " je nakonfigurována pro nepodporovaný datový typ:",
			        BaseCode.SYSTEM_ERROR).set("itemTypeCode", trgItemType.getCode());
		}
		this.trgItemType = trgItemType.getEntity();
		String trgItemSpecCode = config.getTarget().getItemSpec();
		if (trgItemSpecCode != null) {
			this.trgItemSpec = trgItemType.getItemSpecByCode(trgItemSpecCode);
			if(trgItemSpec==null) {
				throw new SystemException(
				        "Hromadná akce " + getName() + " má chybnou specifikaci:",
				        BaseCode.SYSTEM_ERROR).set("itemTypeCode", trgItemType.getCode())
				                .set("specCode", trgItemSpecCode);
			}
		} else {
			if (Boolean.TRUE.equals(this.trgItemType.getUseSpecification())) {
				throw new SystemException(
				        "Hromadná akce " + getName() + " je nakonfigurována bez určení specifikace:",
				        BaseCode.SYSTEM_ERROR).set("itemTypeCode", trgItemType.getCode());
			}
		}
	}

	@Override
	public String getName() {
		return MoveDescItem.class.getSimpleName();
	}

	@Override
	protected void update(ArrLevel level) {
		ArrNode currNode = level.getNode();

		ArrDescItem srcDescItem = loadSingleDescItem(currNode, srcItemType);
		if (srcDescItem != null) {
			ArrDataString srcData = (ArrDataString) srcDescItem.getData();
			// store as new desc item
			ArrDescItem descItem = new ArrDescItem();
			descItem.setItemType(this.trgItemType);
			descItem.setItemSpec(this.trgItemSpec);
			descItem.setNode(currNode);
			ArrDataString trgData = new ArrDataString();
			trgData.setValue(srcData.getValue());
			descItem.setData(trgData);

			/*ArrDescItem trgItem = */
			descriptionItemService.createDescriptionItem(descItem, currNode, version, getChange());
			// delete old one
			descriptionItemService.deleteDescriptionItem(srcDescItem, version, getChange(), true);

			arrangementCacheService.deleteDescItem(currNode.getNodeId(), srcDescItem.getDescItemObjectId());

			// validace uzlu
			ruleService.conformityInfo(version.getFundVersionId(), Arrays.asList(descItem.getNode().getNodeId()),
			        NodeTypeOperation.SAVE_DESC_ITEM, null, null, Arrays.asList(descItem));
		}
	}

	@Override
	protected void done() {
		// No final action

	}

}
