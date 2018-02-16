package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.repository.StructuredItemRepository;

public class UnitCounter {

    final UnitCounterConfig config;

    final StructuredItemRepository structureItemRepository;

    WhenCondition when;

    RuleSystemItemType itemType;
    Map<Integer, String> itemSpecMapping = new HashMap<>();
    RuleSystemItemType itemCount;

    /**
     * Type of item for packets.
     *
     * If null not applied
     */
    private RuleSystemItemType objectType;

    /**
     * Type of item for object mapping.
     */
    private RuleSystemItemType objectItemType;

    /**
     * Packet type mapping
     */
    Map<Integer, String> objectMapping = new HashMap<>();

    /**
     * Již zapracované obaly
     */
    private Set<Integer> countedObjects = new HashSet<>();

    UnitCounter(UnitCounterConfig counterCfg,
            final StructuredItemRepository structureItemRepository,
            RuleSystem ruleSystem) {
        this.config = counterCfg;
        this.structureItemRepository = structureItemRepository;
        init(ruleSystem);
    }

    private void init(RuleSystem ruleSystem) {
        WhenConditionConfig whenConfig = config.getWhen();
        if (whenConfig != null) {
            when = new WhenCondition(whenConfig, ruleSystem);
        }

        // item type with specification
        String itemTypeCode = config.getItemType();
        if (itemTypeCode != null) {
            itemType = ruleSystem.getItemTypeByCode(itemTypeCode);
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
            itemCount = ruleSystem.getItemTypeByCode(itemCountCode);
            Validate.notNull(itemCount);
            Validate.isTrue(itemCount.getDataType() == DataType.INT);
        }

        // object / packet mapping
        String objectTypeCode = config.getObjectType();
        if (objectTypeCode != null) {
            objectType = ruleSystem.getItemTypeByCode(objectTypeCode);
            Validate.notNull(objectType);
            Validate.isTrue(objectType.getDataType() == DataType.STRUCTURED);

            // get item type with packets
            objectItemType = ruleSystem.getItemTypeByCode(config.getObjectItemType());
            Validate.notNull(objectItemType);
            Validate.notNull(objectItemType.getDataType() == DataType.ENUM);

            // prepare packet type mapping
            Map<String, String> packetTypeMapping = config.getObjectItemMapping();
            packetTypeMapping.forEach((packetTypeCode, targetValue) -> {
                RulItemSpec itemSpec = objectItemType.getItemSpecByCode(packetTypeCode);
                Validate.notNull(itemSpec);
                objectMapping.put(itemSpec.getItemSpecId(), targetValue);
            });
        }
    }

    public void apply(LevelWithItems level, UnitCountAction unitCountAction) {
        // check when condition
        if (when != null) {
            if (!when.isTrue(level)) {
                return;
            }
        }
        // stop further processing if set
        if (config.isStopProcessing()) {
            unitCountAction.setSkipSubtree(level);
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
                Integer vCnt = ((ArrDataInteger) data).getValue();
                defaultCount = vCnt;
            }
        }

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
                    Integer vCnt = ((ArrDataInteger) item.getData()).getValue();
                    count = vCnt;
                }
                // get mapping
                String value = itemSpecMapping.get(item.getItemSpecId());
                if (value != null) {
                    unitCountAction.addValue(value, count);
                }
            }
        }

        if (objectType != null) {
            for (ArrDescItem item : level.getDescItems()) {
                // check if type match
                if (!objectType.getItemTypeId().equals(item.getItemTypeId())) {
                    continue;
                }

                // fetch valid items from packet
                Integer packetId = ((ArrDataStructureRef) item.getData()).getStructuredObjectId();
                if (!countedObjects.contains(packetId)) {
                    // TODO: Do filtering in DB
                    List<ArrStructuredItem> structObjItems = this.structureItemRepository
                            .findByStructuredObjectAndDeleteChangeIsNullFetchData(packetId);
                    // filter only our item types
                    for (ArrStructuredItem structObjItem : structObjItems) {
                        if (structObjItem.getItemTypeId().equals(this.objectItemType.getItemTypeId())) {
                            // find mapping
                            String value = objectMapping.get(structObjItem.getItemSpecId());
                            if (value != null) {
                                unitCountAction.addValue(value, 1);

                                // mark as counted
                                countedObjects.add(packetId);
                            }
                        }
                    }
                }
            }
        }
    }
}
