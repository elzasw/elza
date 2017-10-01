package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.TextAggregationActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemFormattedText;
import cz.tacr.elza.domain.ArrItemString;
import cz.tacr.elza.domain.ArrItemText;

/**
 * Akce na agregaci textových hodnot.
 *
 * @author Martin Šlapa
 * @author Petr Pytelka
 * @since 29.06.2016
 */
@Component
@Scope("prototype")
public class TextAggregationAction extends Action {

    /**
     * Oddělovač záznamů
     */
    public static final String DELIMITER = "\n";

    /**
     * Vstupní atributy
     */
	private Map<Integer, RuleSystemItemType> inputItemTypes = new HashMap<>();

    /**
     * Výstupní atribut
     */
	private RuleSystemItemType outputItemType;

    /**
     * Seznam textových hodnot
     */
    private List<String> texts = new ArrayList<>();

	TextAggregationConfig config;

	TextAggregationAction(final TextAggregationConfig config) {
		Validate.notNull(config);
		this.config = config;
    }

    @Override
	public void init(ActionRunContext runContext) {
		RuleSystem ruleSystem = getRuleSystem(runContext);

		String outputType = config.getOutputType();
		outputItemType = ruleSystem.getItemTypeByCode(outputType);
		checkValidDataType(outputItemType, DataType.TEXT);

		for (String inputTypeCode : config.getInputTypes()) {
			RuleSystemItemType inputType = ruleSystem.getItemTypeByCode(inputTypeCode);

			checkValidDataType(inputType, DataType.TEXT, DataType.STRING, DataType.FORMATTED_TEXT);

			inputItemTypes.put(inputType.getItemTypeId(), inputType);

		}
    }

    @Override
	public void apply(LevelWithItems level, TypeLevel typeLevel) {
		List<ArrDescItem> items = level.getDescItems();

		for (ArrItem item : items) {
			// check if item is in inputItemTypes set
			RuleSystemItemType itemType = inputItemTypes.get(item.getItemTypeId());
			if (itemType != null) {
                ArrItemData itemData = item.getItem();

                if (BooleanUtils.isTrue(item.getUndefined())) {
					// skip if not defined
					continue;
                }
                
				String value;
                switch(itemType.getDataType())
                {
				case STRING:
					value = ((ArrItemString) itemData).getValue();
				case TEXT:
					value = (((ArrItemText) itemData).getValue());
                	break;
				case FORMATTED_TEXT:
					value = (((ArrItemFormattedText) itemData).getValue());
				default:
					throw new IllegalStateException(
					        "Neplatný typ dat: " + itemType.getDataType() + ", itemId: " + item.getItemId());
                }
				if (StringUtils.isNotBlank(value)) {
					if (!config.isIgnoreDuplicated() || !texts.contains(value)) {
						texts.add(value);
					}
                }
            }
        }
    }

    @Override
    public ActionResult getResult() {
        // Prepare result
        String resultText = String.join(DELIMITER, texts);

        // Create object with result
        TextAggregationActionResult textAggregationActionResult = new TextAggregationActionResult();
        textAggregationActionResult.setItemType(outputItemType.getCode());
        textAggregationActionResult.setText(resultText);
        // check if not empty
		textAggregationActionResult.setCreateInOutput(config.isCreateEmpty() || !resultText.isEmpty());

        return textAggregationActionResult;
    }

}
