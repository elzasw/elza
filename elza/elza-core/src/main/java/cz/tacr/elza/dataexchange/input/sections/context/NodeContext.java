package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrInhibitedItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import jakarta.validation.constraints.NotNull;

public class NodeContext {

    private final Map<String, ArrDescItem> xmlIdMap = new HashMap<>();

    private final Map<ItemKey, Integer> descItemCount = new HashMap<>();

    private final SectionContext section;

    private final NodeContext parentNodeCtx;

    private final EntityIdHolder<ArrNode> nodeIdHolder;

    private final NodeStorageDispatcher storageDispatcher;

    private final int depth;

    private int levelPosition;

    public NodeContext(SectionContext section, NodeContext parentNodeCtx, EntityIdHolder<ArrNode> nodeIdHolder, NodeStorageDispatcher storageDispatcher, int depth) {
        this.section = Objects.requireNonNull(section);
        this.nodeIdHolder = Objects.requireNonNull(nodeIdHolder);
        this.storageDispatcher = Objects.requireNonNull(storageDispatcher);
        this.parentNodeCtx = parentNodeCtx;
        this.depth = depth;
    }

    public SectionContext getSection() {
        return section;
    }

	public NodeContext getParentNodeCtx() {
		return parentNodeCtx;
	}

	public EntityIdHolder<ArrNode> getIdHolder() {
		return nodeIdHolder;
	}

	public ArrDescItem getDescItemByObjectId(String objectId) {
		return xmlIdMap.get(objectId);
	}

	public NodeContext addChildNode(ArrNode node, String importId) {
        ArrNodeWrapper childNodeWrapper = new ArrNodeWrapper(node);
        ArrLevelWrapper childLevelWrapper = createChildLevelWrapper(childNodeWrapper.getIdHolder());
        return section.addNode(this, childNodeWrapper, childLevelWrapper, importId, depth + 1);
    }

	/**
	 * Search for relevant ArrDescItem by descItemObjectId
	 * 
	 * @param ctx
	 * @param objectId
	 * @return ArrDescItem | null
	 */
	public ArrDescItem findDescItem(NodeContext ctx, String objectId) {
    	ArrDescItem item = ctx.getDescItemByObjectId(objectId);
    	if (item == null && ctx.getParentNodeCtx() != null) {
    		return findDescItem(ctx.getParentNodeCtx(), objectId);
    	}
    	return item;
    }

    /**
     * Stores description item and his data. <br>
     * # Process updates: <br>
     * - Position of item by current count. <br>
     * - Reference between item and data (before persist).
     */
    public void addDescItem(String xmlObjId, ArrDescItem descItem, ArrData data) {
        Validate.isTrue(descItem.isUndefined());
        // set item position
        Integer count = descItemCount.compute(ItemKey.of(descItem), (k, v) -> v == null ? 1 : ++v);
        descItem.setPosition(count);
        // store item & data
        ArrDescItemWrapper descItemWrapper = new ArrDescItemWrapper(descItem, nodeIdHolder);
        if (data != null) {
            Validate.isTrue(data.getDataType() == descItem.getItemType().getDataType());
            ArrDataWrapper dataWrapper = new ArrDataWrapper(data);
            descItemWrapper.setDataIdHolder(dataWrapper.getIdHolder());
            storageDispatcher.addData(dataWrapper, depth);
        }
        storageDispatcher.addDescItem(descItemWrapper, depth);
    	xmlIdMap.put(xmlObjId, descItem);
    }

    /**
     * Stores inhibited item and his refId
     *
     * @param inhtItem
     * @param refItem
     */
    public void addInhibitedItem(ArrInhibitedItem inhtItem, @NotNull String refItem) {
    	ArrDescItem descItem = findDescItem(this, refItem);
    	Objects.requireNonNull(descItem, "Incorrect refItem: " + refItem);

    	ArrInhibitedItemWrapper inhItemWrapper = new ArrInhibitedItemWrapper(inhtItem, nodeIdHolder, descItem.getDescItemObjectId());
    	storageDispatcher.addInhibitedItem(inhItemWrapper, depth);
    }

    private ArrLevelWrapper createChildLevelWrapper(EntityIdHolder<ArrNode> childNodeIdHolder) {
        levelPosition++;
        return createLevelWrapper(childNodeIdHolder, nodeIdHolder, levelPosition, section.getCreateChange());
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
