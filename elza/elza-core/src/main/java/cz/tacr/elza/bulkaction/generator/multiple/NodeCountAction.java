package cz.tacr.elza.bulkaction.generator.multiple;

import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.NodeCountActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrBulkActionRun;

/**
 * Akce na počítání počtu uzlů.
 *
 */
@Component
@Scope("prototype")
public class NodeCountAction extends Action {

    /**
     * Výstupní atribut
     */
	private ItemType outputItemType;

    /**
     * Skip subtree
     */
    LevelWithItems skipSubtree;

    /**
     * Počet procházených uzlů
     */
    private Integer count = 0;

	NodeCountConfig config;

    private WhenCondition excludeWhen;

	NodeCountAction(final NodeCountConfig config) {
		Validate.notNull(config);
		this.config = config;
    }

    @Override
    public void init(BulkAction bulkAction, ArrBulkActionRun bulkActionRun) {
        super.init(bulkAction, bulkActionRun);

        StaticDataProvider sdp = getStaticDataProvider();

        // initialize exclude configuration
        WhenConditionConfig excludeWhenConfig = config.getExcludeWhen();
        if (excludeWhenConfig != null) {
            excludeWhen = new WhenCondition(excludeWhenConfig, sdp);
        }

		String outputType = config.getOutputType();
        outputItemType = sdp.getItemTypeByCode(outputType);

		checkValidDataType(outputItemType, DataType.INT);
    }

    /**
     * Mark level to be skipped
     * 
     * @param level
     */
    public void setSkipSubtree(LevelWithItems level) {
        this.skipSubtree = level;
    }

    @Override
	public void apply(LevelWithItems level, TypeLevel typeLevel) {
        // Check if node stopped
        if (skipSubtree != null) {
            if (isInTree(skipSubtree, level)) {
                return;
            }
            // reset limit
            skipSubtree = null;
        }

        // check exclude condition
        if (excludeWhen != null) {
            if (excludeWhen.isTrue(level)) {
                // set as skip
                setSkipSubtree(level);
                return;
            }
        }

        // we are counting only children
		if (typeLevel.equals(TypeLevel.CHILD)) {
			count++;
        }
    }

    @Override
    public ActionResult getResult() {
        NodeCountActionResult nodeCountResult = new NodeCountActionResult();
        nodeCountResult.setItemType(outputItemType.getCode());
        nodeCountResult.setCount(count);
        return nodeCountResult;
    }

}
