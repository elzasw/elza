package cz.tacr.elza.bulkaction.generator.multiple;

import cz.tacr.elza.core.data.StaticDataProvider;
import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.NodeCountActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
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
     * Počet procházených uzlů
     */
    private Integer count = 0;

	NodeCountConfig config;

	NodeCountAction(final NodeCountConfig config) {
		Validate.notNull(config);
		this.config = config;
    }

    @Override
	public void init(ArrBulkActionRun bulkActionRun) {
		StaticDataProvider ruleSystem = getStaticDataProvider();

		String outputType = config.getOutputType();
		outputItemType = ruleSystem.getItemTypeByCode(outputType);

		checkValidDataType(outputItemType, DataType.INT);
    }

    @Override
	public void apply(LevelWithItems level, TypeLevel typeLevel) {
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
