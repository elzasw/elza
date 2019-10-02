package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;

public class NodeContext {

    private final Map<ItemKey, Integer> descItemCount = new HashMap<>();

    private final SectionContext section;

    private final EntityIdHolder<ArrNode> idHolder;

    private final NodeStorageDispatcher storageDispatcher;

    private final int depth;

    private int levelPosition;

    NodeContext(SectionContext section, EntityIdHolder<ArrNode> nodeIdHolder, NodeStorageDispatcher storageDispatcher,
            int depth) {
        this.section = Validate.notNull(section);
        this.idHolder = Validate.notNull(nodeIdHolder);
        this.storageDispatcher = Validate.notNull(storageDispatcher);
        this.depth = depth;
    }

    public SectionContext getSection() {
        return section;
    }

    public NodeContext addChildNode(ArrNode node, String importId) {
        ArrNodeWrapper childNodeWrapper = new ArrNodeWrapper(node);
        ArrLevelWrapper childLevelWrapper = createChildLevelWrapper(childNodeWrapper.getIdHolder());
        return section.addNode(childNodeWrapper, childLevelWrapper, importId, depth + 1);
    }

    /**
     * Stores description item and his data. <br>
     * # Process updates: <br>
     * - Position of item by current count. <br>
     * - Reference between item and data (before persist).
     */
    public void addDescItem(ArrDescItem descItem, ArrData data) {
        Validate.isTrue(descItem.isUndefined());
        // set item position
        Integer count = descItemCount.compute(ItemKey.of(descItem), (k, v) -> v == null ? 1 : ++v);
        descItem.setPosition(count);
        // store item & data
        ArrDescItemWrapper descItemWrapper = new ArrDescItemWrapper(descItem, idHolder);
        if (data != null) {
            Validate.isTrue(data.getDataType() == descItem.getItemType().getDataType());
            ArrDataWrapper dataWrapper = new ArrDataWrapper(data);
            descItemWrapper.setDataIdHolder(dataWrapper.getIdHolder());
            storageDispatcher.addData(dataWrapper, depth);
        }
        storageDispatcher.addDescItem(descItemWrapper, depth);
    }

    private ArrLevelWrapper createChildLevelWrapper(EntityIdHolder<ArrNode> childNodeIdHolder) {
        levelPosition++;
        return createLevelWrapper(childNodeIdHolder, idHolder, levelPosition, section.getCreateChange());
    }

    public static ArrLevelWrapper createLevelWrapper(EntityIdHolder<ArrNode> nodeIdHolder,
                                                     EntityIdHolder<ArrNode> parentNodeIdHolder,
                                                     int position,
                                                     ArrChange createChange) {
        ArrLevel level = new ArrLevel();
        level.setPosition(position);
        level.setCreateChange(createChange);
        return new ArrLevelWrapper(level, nodeIdHolder, parentNodeIdHolder);
    }
}
