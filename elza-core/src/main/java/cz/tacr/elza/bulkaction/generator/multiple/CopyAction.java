package cz.tacr.elza.bulkaction.generator.multiple;

import com.google.common.base.Objects;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.CopyActionResult;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.utils.Yaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Akce pro kopírování hodnot atributů.
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
@Component
@Scope("prototype")
public class CopyAction extends Action {

    /**
     * Vstupní atributy
     */
    private Set<RulItemType> inputItemTypes;

    /**
     * Výstupní atribut
     */
    private RulItemType outputItemType;

    /**
     * Seznam kopírovaných hodnot atributů
     */
    private List<ArrDescItem> dataItems = new ArrayList<>();

    /**
     * Provést distinct při vracení výsledků?
     */
    private boolean distinct;

    @Autowired
    private DescItemFactory descItemFactory;

    CopyAction(final Yaml config) {
        super(config);
    }

    @Override
    public void init() {
        Set<String> inputTypes = config.getStringList("input_types", null).stream().collect(Collectors.toSet());
        String outputType = config.getString("output_type", null);
        distinct = config.getBoolean("distinct", false);

        inputItemTypes = findItemTypes(inputTypes);
        outputItemType = findItemType(outputType, "output_type");

        String code = outputItemType.getDataType().getCode();
        for (RulItemType inputItemType : inputItemTypes) {
            if (!inputItemType.getDataType().getCode().equals(code)) {
                throw new BusinessException("Atributy " + inputItemType.getCode() + " a " + outputItemType.getCode() + " nemají stejný datový typ", BaseCode.ID_NOT_EXIST);
            }
        }
    }

    @Override
    public void apply(final ArrNode node, final List<ArrDescItem> items, final LevelWithItems parentLevelWithItems) {
        for (ArrDescItem item : items) {
            // pouze hledaný typ
            if (inputItemTypes.contains(item.getItemType())) {
                if (distinct) {
                    boolean exist = false;
                    for (ArrDescItem descItem : dataItems) {
                        if (Objects.equal(item.getFulltextValue(), descItem.getFulltextValue())) {
                            exist = true;
                            break;
                        }
                    }
                    if (!exist) {
                        dataItems.add(item);
                    }
                } else {
                    dataItems.add(item);
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
        CopyActionResult copyActionResult = new CopyActionResult();
        copyActionResult.setItemType(outputItemType.getCode());
        copyActionResult.setDataItems(new ArrayList<>(dataItems));
        return copyActionResult;
    }

}
