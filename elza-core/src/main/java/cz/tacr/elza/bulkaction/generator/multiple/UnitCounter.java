package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.StructuredItemRepository;

public class UnitCounter {

    final UnitCounterConfig config;

    final StructuredItemRepository structureItemRepository;

    WhenCondition excludeWhen;

    WhenCondition when;

    ItemType itemType;
    Map<Integer, String> itemSpecMapping = new HashMap<>();
    ItemType itemCount;

    /**
     * Type of item for packets.
     *
     * If null not applied
     */
    private ItemType srcStructObjType;

    /**
     * Type of item for structured object mapping.
     */
    private ItemType objectItemType;

    /**
     * Packet type mapping
     */
    Map<Integer, String> objectMapping = new HashMap<>();
    
    UnitCounter(UnitCounterConfig counterCfg,
                final StructuredItemRepository structureItemRepository,
                StaticDataProvider sdp) {
        this.config = counterCfg;
        this.structureItemRepository = structureItemRepository;
        init(sdp);
    }

    private void init(StaticDataProvider spd) {
        // initialize exclude configuration
        WhenConditionConfig excludeWhenConfig = config.getExcludeWhen();
        if (excludeWhenConfig != null) {
            excludeWhen = new WhenCondition(excludeWhenConfig, spd);
        }

        WhenConditionConfig whenConfig = config.getWhen();
        if (whenConfig != null) {
            when = new WhenCondition(whenConfig, spd);
        }

        // item type with specification
        String itemTypeCode = config.getItemType();
        if (itemTypeCode != null) {
            itemType = spd.getItemTypeByCode(itemTypeCode);
            Validate.notNull(itemType);
            Validate.isTrue(itemType.hasSpecifications());
            // only enums and INTs are supported
            Validate.isTrue(itemType.getDataType() == DataType.ENUM || itemType.getDataType() == DataType.INT);

            // prepare mapping
            Map<String, String> specConfig = config.getItemSpecMapping();
            specConfig.forEach((a, b) -> {
                RulItemSpec spec = itemType.getItemSpecByCode(a);
                Validate.notNull(spec, "Cannot find specification: " + a);
                itemSpecMapping.put(spec.getItemSpecId(), b);
            });
        }

        String itemCountCode = config.getItemCount();
        if (itemCountCode != null) {
            itemCount = spd.getItemTypeByCode(itemCountCode);
            Validate.notNull(itemCount);
            Validate.isTrue(itemCount.getDataType() == DataType.INT);
        }

        // object / packet mapping
        String objectTypeCode = config.getObjectType();
        if (objectTypeCode != null) {
            srcStructObjType = spd.getItemTypeByCode(objectTypeCode);
            Validate.notNull(srcStructObjType);
            Validate.isTrue(srcStructObjType.getDataType() == DataType.STRUCTURED);

            // get item type with packets
            objectItemType = spd.getItemTypeByCode(config.getObjectItemType());
            Validate.notNull(objectItemType);
            Validate.notNull(objectItemType.getDataType() == DataType.ENUM);

            // prepare packet type mapping
            Map<String, String> packetTypeMapping = config.getObjectItemMapping();
            packetTypeMapping.forEach((packetTypeCode, targetValue) -> {
                RulItemSpec itemSpec = objectItemType.getItemSpecByCode(packetTypeCode);
                if (itemSpec == null) {
                    throw new BusinessException("Missing specification: " + packetTypeCode, BaseCode.ID_NOT_EXIST)
                            .set("packetTypeCode", packetTypeCode);
                }
                objectMapping.put(itemSpec.getItemSpecId(), targetValue);
            });
        }
    }

    public void apply(LevelWithItems level, UnitCountAction unitCountAction) {
        // check exclude condition
        if (excludeWhen != null) {
            if (excludeWhen.isTrue(level)) {
                // set as skip
                unitCountAction.setSkipSubtree(level, null);
                return;
            }
        }

        // check when condition
        if (when != null) {
            if (!when.isTrue(level)) {
                return;
            }
        }

        // read default count from extra item
        int defaultCount = 1;
        if (itemCount != null) {
            for (ArrDescItem item : level.getDescItems()) {
                // check if type match
                if (!itemCount.getItemTypeId().equals(item.getItemTypeId())) {
                    continue;
                }
                ArrData data = item.getData();
                Integer vCnt = ((ArrDataInteger) data).getIntegerValue();
                defaultCount = vCnt;
            }
        }

        Consumer<LevelWithItems> lastUsedDetailCounter = null;

        // prepare output
        if (itemType != null) {
            for (ArrDescItem item : level.getDescItems()) {
                // check if type match
                if (!itemType.getItemTypeId().equals(item.getItemTypeId())) {
                    continue;
                }
                // count
                int count = defaultCount;
                // read count from int value
                if (itemType.getDataType() == DataType.INT) {
                    Integer vCnt = ((ArrDataInteger) item.getData()).getIntegerValue();
                    count = vCnt;
                }
                // get mapping
                Consumer<LevelWithItems> detailCountAction = null;
                String value = itemSpecMapping.get(item.getItemSpecId());
                if (value != null) {
                    if (unitCountAction.isLocal()) {
                        unitCountAction.createDescItem(level, value, count);
                    } else {
                        detailCountAction = unitCountAction.addValue(level, value, count);
                    }
                }

                if (detailCountAction != null) {
                    detailCountAction.accept(level);

                    lastUsedDetailCounter = detailCountAction;
                }
            }
        }

        // StructObj mapping
        if (srcStructObjType != null) {
            Consumer<LevelWithItems> usedDetailCounter = countStructObjs(srcStructObjType, level, unitCountAction);
            if (usedDetailCounter != null) {
                lastUsedDetailCounter = usedDetailCounter;
            }
        }

        // stop further processing if set
        if (config.isStopProcessing()) {
            unitCountAction.setSkipSubtree(level, lastUsedDetailCounter);
        }
    }

    /**
     * Count from given structured object
     * 
     * @param itemType
     * @param Level.get
     * @return
     */
    private Consumer<LevelWithItems> countStructObjs(@Nonnull ItemType itemType, LevelWithItems level,
                                UnitCountAction unitCountAction) {

        // List<ArrDescItem> descItems = level.getDescItems();
        List<ArrDescItem> descItems = level.getInheritedDescItems(itemType);
        if (CollectionUtils.isEmpty(descItems)) {
            return null;
        }
        Consumer<LevelWithItems> lastUsedDetailCounter = null;
        for (ArrDescItem item : descItems) {
            // fetch valid items from packet
            ArrDataStructureRef dataStructObjRef = HibernateUtils.unproxy(item.getData());
            Integer packetId = dataStructObjRef.getStructuredObjectId();
            Consumer<LevelWithItems> detailCounter = countStructObj(packetId, level, unitCountAction);
            if (detailCounter != null) {
                lastUsedDetailCounter = detailCounter;
            }
        }
        return lastUsedDetailCounter;
    }

    /**
     * Count structured object
     * 
     * @param packetId
     * @param level
     * @param unitCountAction
     * @return Return detail counter to be applied on the level
     */
    private Consumer<LevelWithItems> countStructObj(Integer packetId, LevelWithItems level,
                                                    UnitCountAction unitCountAction) {
        if (unitCountAction.isCountedObject(packetId)) {
            Consumer<LevelWithItems> detailCounter = unitCountAction.getCountedAction(packetId);
            if (detailCounter != null) {
                detailCounter.accept(level);
            }
            return detailCounter;
        }

        // TODO: Do filtering in DB
        List<ArrStructuredItem> structObjItems = this.structureItemRepository
                .findByStructObjIdAndDeleteChangeIsNullFetchData(packetId);
        // filter only our item types
        for (ArrStructuredItem structObjItem : structObjItems) {
            if (structObjItem.getItemTypeId().equals(this.objectItemType.getItemTypeId())) {
                // find mapping
                String value = objectMapping.get(structObjItem.getItemSpecId());
                if (value != null) {
                    Consumer<LevelWithItems> detailCounter;
                    if (unitCountAction.isLocal()) {
                        unitCountAction.createDescItem(level, value, 1);
                        detailCounter = null;
                    } else {
                        detailCounter = unitCountAction.addValue(level, value, 1);
                    }

                    // mark as counted
                    unitCountAction.addCountedObject(packetId, detailCounter);
                    // run extra action
                    if (detailCounter != null) {
                        detailCounter.accept(level);
                    }
                    // only first mapping is used and then return
                    // if there are multiple structObjItems matching
                    // given itemType only first one is counted
                    return detailCounter;
                }
            }
        }

        return null;
    }
}
