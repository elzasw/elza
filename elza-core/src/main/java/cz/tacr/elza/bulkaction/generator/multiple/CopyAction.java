package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.CopyActionResult;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Akce pro kopírování hodnot atributů.
 *
 */
@Component
@Scope("prototype")
public class CopyAction extends Action {

    /**
     * Vstupní atributy
     */
	private Map<Integer, RuleSystemItemType> inputItemTypes = new HashMap<>();

    /**
     * Výstupní atribut
     */
	private RuleSystemItemType outputItemType;

    /**
	 * Zkopírované hodnoty, výsledek
	 */
    private List<ArrDescItem> dataItems = new ArrayList<>();

	final CopyConfig config;

	CopyAction(final CopyConfig config) {
		Validate.notNull(config);
		this.config = config;
    }

    @Override
	public void init(ArrBulkActionRun bulkActionRun) {
		RuleSystem ruleSystem = getRuleSystem(bulkActionRun);

		String outputType = config.getOutputType();
		outputItemType = ruleSystem.getItemTypeByCode(outputType);

		for (String inputTypeCode : config.getInputTypes()) {
			RuleSystemItemType inputType = ruleSystem.getItemTypeByCode(inputTypeCode);

			// check if input and output have same data type
			if (outputItemType.getDataType() != inputType.getDataType()) {
				throw new BusinessException(
				        "Item " + inputTypeCode + " and " + outputType + " have different data type",
				        BaseCode.PROPERTY_HAS_INVALID_TYPE);
			}

			inputItemTypes.put(inputType.getItemTypeId(), inputType);

		}
    }

	/**
	 * Check if specification is used in output
	 *
	 * @param itemSpecId
	 * @return
	 */
	private boolean isSpecificationUsed(Integer itemSpecId) {
		for (ArrDescItem dataItem : dataItems) {
			RulItemSpec spec = dataItem.getItemSpec();
			Integer currSpecd = null;
			if (spec != null) {
				currSpecd = spec.getItemSpecId();
			}
			if (Objects.equals(itemSpecId, currSpecd)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void apply(LevelWithItems level, TypeLevel typeLevel) {
		List<ArrDescItem> items = level.getDescItems();

        for (ArrDescItem item : items) {
			// check if item is in inputItemTypes set
			RuleSystemItemType itemType = inputItemTypes.get(item.getItemTypeId());
			if (itemType == null) {
				continue;
			}
			// skip undefined items
			if (item.isUndefined()) {
				continue;
			}
			// check if exists
			if (config.isDistinct()) {
				Integer itemSpecId = item.getItemSpecId();
				if (isSpecificationUsed(itemSpecId)) {
					continue;
				}
			}

			// Copy item
			// TODO: Ma zde byt?, nutno overit
			/*
			ArrData itemData = item.getData();
			if (itemData == null) {
				itemData = descItemFactory.createItemByType(item.getItemType().getDataType());
			}
			itemData.setSpec(item.getItemSpec());
			*/
			dataItems.add(item);
        }
	}

    @Override
    public ActionResult getResult() {
        CopyActionResult copyActionResult = new CopyActionResult();
        copyActionResult.setItemType(outputItemType.getCode());
        copyActionResult.setDataItems(new ArrayList<>(dataItems));
        return copyActionResult;
    }


}
