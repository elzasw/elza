package cz.tacr.elza.bulkaction.generator.multiple;

import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.NodeCountActionResult;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.utils.Yaml;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Akce na počítání počtu uzlů.
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
@Component
@Scope("prototype")
public class NodeCountAction extends Action {

    /**
     * Výstupní atribut
     */
    private RulItemType outputItemType;

    /**
     * Počet procházených uzlů
     */
    private Integer count = 0;

    NodeCountAction(final Yaml config) {
        super(config);
    }

    @Override
    public void init() {
        String outputType = config.getString("output_type", null);

        outputItemType = findItemType(outputType, "output_type");
        checkValidDataType(outputItemType, "INT");
    }

    @Override
    public void apply(final ArrNode node, final List<ArrDescItem> items, final Map<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        count++;
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
        NodeCountActionResult nodeCountResult = new NodeCountActionResult();
        nodeCountResult.setItemType(outputItemType.getCode());
        nodeCountResult.setCount(count);
        return nodeCountResult;
    }

}
