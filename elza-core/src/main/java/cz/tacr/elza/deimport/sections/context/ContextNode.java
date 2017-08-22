package cz.tacr.elza.deimport.sections.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import cz.tacr.elza.deimport.context.IdHolder;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;

public class ContextNode {

    private final Map<DescItemKey, Integer> descItemCount = new HashMap<>();

    private final ContextSection section;

    private final IdHolder nodeIdHolder;

    private final int depth;

    private int levelPosition;

    ContextNode(ContextSection section, IdHolder nodeIdHolder, int depth) {
        this.section = Objects.requireNonNull(section);
        this.nodeIdHolder = Objects.requireNonNull(nodeIdHolder);
        this.depth = depth;
    }

    public ContextSection getSection() {
        return section;
    }

    public ContextNode addChildNode(ArrNode node, String importId) {
        ArrNodeWrapper childNodeWrapper = new ArrNodeWrapper(node);
        ArrLevelWrapper childLevelWrapper = createChildLevelWrapper(childNodeWrapper.getIdHolder());
        return section.addNode(childNodeWrapper, childLevelWrapper, importId, depth + 1);
    }

    public void addNodeRegister(ArrNodeRegister nodeRegister) {
        ArrNodeRegisterWrapper wrapper = new ArrNodeRegisterWrapper(nodeRegister, nodeIdHolder);
        section.addNodeRegister(wrapper, depth);
    }

    /**
     * Stores description item and his data. <br>
     * # Process updates: <br>
     * - Data type for data by description item. <br>
     * - Position of description item by current item count. <br>
     * - Reference between description item and data during before persist handler.
     */
    public void addDescItem(ArrDescItem descItem, ArrData data) {
        // set data type
        data.setDataType(descItem.getItemType().getDataType());
        // set item position
        Integer count = descItemCount.compute(DescItemKey.of(descItem), (k, v) -> v == null ? 1 : ++v);
        descItem.setPosition(count);
        // store item and data
        ArrDescItemWrapper itemWrapper = new ArrDescItemWrapper(descItem, nodeIdHolder);
        ArrDataWrapper dataWrapper = new ArrDataWrapper(data, itemWrapper.getIdHolder());
        section.addDescItem(itemWrapper, dataWrapper, depth);
    }

    private ArrLevelWrapper createChildLevelWrapper(IdHolder childNodeIdHolder) {
        levelPosition++;
        return createLevelWrapper(childNodeIdHolder, nodeIdHolder, levelPosition, section.getCreateChange());
    }

    public static ArrLevelWrapper createLevelWrapper(IdHolder nodeIdHolder,
                                                     IdHolder parentNodeIdHolder,
                                                     int position,
                                                     ArrChange createChange) {
        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        return new ArrLevelWrapper(level, nodeIdHolder, parentNodeIdHolder);
    }
}