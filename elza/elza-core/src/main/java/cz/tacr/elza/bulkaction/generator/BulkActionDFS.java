package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.eventnotification.EventNotificationService;

/**
 * Base implementation for Depth First Search
 * bulk action. This action is pre-order.
 *
 */
public abstract class BulkActionDFS extends BulkAction {

	protected Result result;

    @Autowired
    protected NodeCacheService nodeCacheService;

    @Autowired
    protected RuleService ruleService;

    @Autowired
    protected EventNotificationService notificationService;

    ActionRunContext runContext;

	@Override
	public void run(ActionRunContext runContext) {

        this.runContext = runContext;

		result = new Result();

        for (Integer nodeId : runContext.getInputNodeIds()) {
            ArrNode nodeRef = nodeRepository.getOne(nodeId);
            ArrLevel level = levelRepository.findByNodeAndDeleteChangeIsNull(nodeRef);
            Validate.notNull(level);

            run(level);
		}

        if (multipleItemChangeContext != null) {
            multipleItemChangeContext.flush();
        }

		done();

		bulkActionRun.setResult(result);

	}

	/**
	 * Generování hodnot - rekurzivní volání pro procházení celého stromu
	 *
	 * @param level
	 *            uzel
	 * @param rootNode
	 */
	protected void run(ArrLevel level) {
		if (bulkActionRun.isInterrupted()) {
			bulkActionRun.setState(State.INTERRUPTED);
			throw new BusinessException("Hromadná akce " + getName() + " byla přerušena.",
			        ArrangementCode.BULK_ACTION_INTERRUPTED).set("code", bulkActionRun.getBulkActionCode());
		}

		// update serial number
		update(level);

		List<ArrLevel> childLevels = getChildren(level);

		for (ArrLevel childLevel : childLevels) {
			run(childLevel);
		}
	}

	protected abstract void update(ArrLevel level);

	/**
	 * Prepare result
	 */
	protected abstract void done();
}
