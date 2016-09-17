package cz.tacr.elza.bulkaction.generator.multiple;

import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.TextAggregationActionResult;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemFormattedText;
import cz.tacr.elza.domain.ArrItemString;
import cz.tacr.elza.domain.ArrItemText;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.utils.Yaml;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Akce na agregaci textových hodnot.
 *
 * @author Martin Šlapa
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

    TextAggregationAction(final Yaml config) {
        super(config);
    }

    @Override
    public void init() {
        Set<String> inputTypes = config.getStringList("input_types", null).stream().collect(Collectors.toSet());
        String outputType = config.getString("output_type", null);
        ignoreDuplicated = config.getBoolean("ignore_duplicated", false);

        inputItemTypes = findItemTypes(inputTypes);
        for (RulItemType inputItemType : inputItemTypes) {
            checkValidDataType(inputItemType, "TEXT", "STRING", "FORMATTED_TEXT");
        }

        outputItemType = findItemType(outputType);
        checkValidDataType(outputItemType, "TEXT");
    }

    @Override
    public void apply(final ArrNode node, final List<ArrDescItem> items, final Map<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        for (ArrItem item : items) {
            if (inputItemTypes.contains(item.getItemType())) {
                ArrItemData itemData = item.getItem();
                String value;
                if (itemData instanceof ArrItemString) {
                    value = ((ArrItemString) itemData).getValue();
                } else if (itemData instanceof ArrItemText) {
                    value = (((ArrItemText) itemData).getValue());
                } else if (itemData instanceof ArrItemFormattedText) {
                    value = (((ArrItemFormattedText) itemData).getValue());
                } else {
                    throw new IllegalStateException("Neplatmý typ dat: " + itemData.getClass().getSimpleName());
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
        TextAggregationActionResult textAggregationActionResult = new TextAggregationActionResult();
        textAggregationActionResult.setItemType(outputItemType.getCode());
        textAggregationActionResult.setText(String.join(DELIMITER, texts));
        return textAggregationActionResult;
    }

}
