package cz.tacr.elza.bulkaction.generator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.vo.NodeTypeOperation;
import cz.tacr.elza.service.ArrangementCacheService;
import cz.tacr.elza.service.RuleService;

@Component
@Scope("prototype")
public class MoveDescItem extends BulkActionDFS {

    private static final String ITEM_TYPE_CODE = "itemTypeCode";

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
		ItemType srcItemType = staticDataProvider.getItemTypeByCode(config.getSource().getItemType());
		Validate.notNull(srcItemType);

		// check if supported source data type
		if (srcItemType.getDataType() != DataType.STRING) {
            throw createConfigException("Source item type has unsupported data type.")
                    .set(ITEM_TYPE_CODE, this.srcItemType.getCode());
		}

		this.srcItemType = srcItemType.getEntity();

		// prepare target type
		ItemType trgItemType = staticDataProvider.getItemTypeByCode(config.getTarget().getItemType());
		Validate.notNull(trgItemType);
		if (trgItemType.getDataType() != DataType.STRING) {
            throw createConfigException("Target item type has unsupported data type.")
                    .set(ITEM_TYPE_CODE, this.trgItemType.getCode());
		}
		this.trgItemType = trgItemType.getEntity();

        // check specification
		String trgItemSpecCode = config.getTarget().getItemSpec();
		if (trgItemSpecCode != null) {
			this.trgItemSpec = trgItemType.getItemSpecByCode(trgItemSpecCode);
			if(trgItemSpec==null) {
                throw createConfigException("Target item type has unknown specification.")
                        .set(ITEM_TYPE_CODE, this.trgItemType.getCode())
                                .set("itemSpecCode", trgItemSpecCode);
			}
		} else {
			if (Boolean.TRUE.equals(this.trgItemType.getUseSpecification())) {
                throw createConfigException("Target item type has to have specification.")
                        .set(ITEM_TYPE_CODE, this.trgItemType.getCode());
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
			trgData.setStringValue(srcData.getStringValue());
			descItem.setData(trgData);

			/*ArrDescItem trgItem = */
			descriptionItemService.createDescriptionItem(descItem, currNode, version, getChange());
			// delete old one
            List<ArrDescItem> items = Collections.singletonList(srcDescItem);
            descriptionItemService.deleteDescriptionItems(items, currNode, version, getChange(), true);

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
