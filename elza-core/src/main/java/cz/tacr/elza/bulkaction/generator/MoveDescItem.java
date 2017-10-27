package cz.tacr.elza.bulkaction.generator;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;

@Component
@Scope("prototype")
public class MoveDescItem extends BulkActionDFS {

	@Autowired
	private StaticDataService staticDataService;

	MoveDescItemConfig config;

	/**
	 * Source item type
	 */
	private RulItemType srcItemType;

	public MoveDescItem(MoveDescItemConfig moveDescItemConfig) {
		this.config = moveDescItemConfig;
	}

	@Override
	protected void init() {
		StaticDataProvider sdp = staticDataService.getData();
		RuleSystem ruleSystem = sdp.getRuleSystems().getByRuleSetCode(config.getRules());
		Validate.notNull(ruleSystem, "Rule system not available, code:" + config.getRules());

		// prepare item type
		RuleSystemItemType srcItemType = ruleSystem.getItemTypeByCode(config.getSource().getItemType());
		Validate.notNull(srcItemType);

		this.srcItemType = srcItemType.getEntity();
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
			// store as new desc item
			// delete old one
		}
	}

	@Override
	protected void done() {
		// TODO Auto-generated method stub

	}

}
