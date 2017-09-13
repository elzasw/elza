package cz.tacr.elza.bulkaction.generator.multiple;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.TextAggregationActionResult;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.utils.Yaml;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private Set<RulItemType> inputItemTypes;

    /**
     * Výstupní atribut
     */
    private RulItemType outputItemType;

    /**
     * Seznam textových hodnot
     */
    private List<String> texts = new ArrayList<>();

    /**
     * Ignorovat duplikáty?
     */
    private boolean ignoreDuplicated;

    /**
     * Flag if text item should be created for empty result
     */
    private boolean createEmpty;

    TextAggregationAction(final Yaml config) {
        super(config);
    }

    @Override
    public void init() {
        Set<String> inputTypes = new HashSet<>(config.getStringList("input_types", null));
        String outputType = config.getString("output_type", null);
        ignoreDuplicated = config.getBoolean("ignore_duplicated", false);
        createEmpty = config.getBoolean("create_empty", true);

        inputItemTypes = findItemTypes(inputTypes);
        for (RulItemType inputItemType : inputItemTypes) {
            checkValidDataType(inputItemType, "TEXT", "STRING", "FORMATTED_TEXT");
        }

        outputItemType = findItemType(outputType, "output_type");
        checkValidDataType(outputItemType, "TEXT");
    }

    @Override
    public void apply(final ArrNode node, final List<ArrDescItem> items, final LevelWithItems parentLevelWithItems) {
        for (ArrItem item : items) {
            if (inputItemTypes.contains(item.getItemType())) {
                ArrData data = item.getData();
                String value;
                if (BooleanUtils.isTrue(item.isUndefined())) {
                    value = ArrangementService.UNDEFINED;
                } else if (data instanceof ArrDataString) {
                    value = ((ArrDataString) data).getValue();
                } else if (data instanceof ArrDataText) {
                    value = (((ArrDataText) data).getValue());
                } else {
                    throw new IllegalStateException("Neplatmý typ dat: " + data.getClass().getSimpleName());
                }
                if (!ignoreDuplicated || !texts.contains(value)) {
                    texts.add(value);
                }
            }
        }
    }

    @Override
    public boolean canApply(final TypeLevel typeLevel) {
        if (typeLevel.equals(TypeLevel.PARENT) && applyParents) {
            return true;
        }

        if (typeLevel.equals(TypeLevel.CHILD) && applyChildren) {
            return true;
        }

        return false;
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
        textAggregationActionResult.setCreateInOutput(createEmpty || !resultText.isEmpty());

        return textAggregationActionResult;
    }

}
