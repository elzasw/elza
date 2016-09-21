package cz.tacr.elza.bulkaction.generator.multiple;

import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.DataceRangeActionResult;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemUnitdate;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.utils.Yaml;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Akce na zjištění rozsahu datací.
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
@Component
@Scope("prototype")
public class DataceRangeAction extends Action {

    /**
     * Vstupní atributy
     */
    private Set<RulItemType> inputItemTypes;

    /**
     * Výstupní atribut
     */
    private RulItemType outputItemType;

    /**
     * Minimální čas
     */
    private Long dataceMin = Long.MAX_VALUE;

    /**
     * Maximální čas
     */
    private Long dataceMax = Long.MIN_VALUE;

    DataceRangeAction(final Yaml config) {
        super(config);
    }

    @Override
    public void init() {
        Set<String> inputTypes = config.getStringList("input_types", null).stream().collect(Collectors.toSet());
        String outputType = config.getString("output_type", null);

        inputItemTypes = findItemTypes(inputTypes);
        for (RulItemType inputItemType : inputItemTypes) {
            checkValidDataType(inputItemType, "UNITDATE");
        }

        outputItemType = findItemType(outputType);
        checkValidDataType(outputItemType, "TEXT");
    }

    @Override
    public void apply(final ArrNode node, final List<ArrDescItem> items, final Map<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        for (ArrItem item : items) {
            if (inputItemTypes.contains(item.getItemType())) {
                ArrItemUnitdate data = (ArrItemUnitdate) item.getItem();
                if (dataceMin > data.getNormalizedFrom()) {
                    dataceMin = data.getNormalizedFrom();
                }
                if (dataceMax < data.getNormalizedTo()) {
                    dataceMax = data.getNormalizedTo();
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
        DataceRangeActionResult dataceRangeResult = new DataceRangeActionResult();

        dataceRangeResult.setItemType(outputItemType.getCode());

        // TODO slapa
        dataceRangeResult.setText("TODO <" + dataceMin + ";" + dataceMax + ">");

        return dataceRangeResult;
    }


}
