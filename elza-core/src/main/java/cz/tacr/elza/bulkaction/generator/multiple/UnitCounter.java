package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulPacketType;

public class UnitCounter {

	final UnitCounterConfig config;

    WhenCondition excludeWhen;

	WhenCondition when;

	RuleSystemItemType itemType;
	Map<Integer, String> itemSpecMapping = new HashMap<>();
	RuleSystemItemType itemCount;

	/**
	 * Type of item for object mapping.
	 * 
	 * If null not applied
	 */
	private RuleSystemItemType objectType;
	/**
	 * Packet type mapping
	 */
	Map<Integer, String> objectMapping = new HashMap<>();

	/**
	 * Již zapracované obaly
	 */
	private Set<Integer> countedObjects = new HashSet<>();

	UnitCounter(UnitCounterConfig counterCfg, RuleSystem ruleSystem) {
		this.config = counterCfg;
		init(ruleSystem);
	}

	private void init(RuleSystem ruleSystem) {
        // initialize exclude configuration
        WhenConditionConfig excludeWhenConfig = config.getExcludeWhen();
        if (excludeWhenConfig != null) {
            excludeWhen = new WhenCondition(excludeWhenConfig, ruleSystem);
        }

		WhenConditionConfig whenConfig = config.getWhen();
		if (whenConfig != null) {
			when = new WhenCondition(whenConfig, ruleSystem);
		}

		// item type with specification
		String itemTypeCode = config.getItemType();
		if(itemTypeCode!=null) {
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
			Validate.isTrue(objectType.getDataType() == DataType.PACKET_REF);

			// prepare packet type mapping
			Map<String, String> packetTypeMapping = config.getObjectMapping();
			packetTypeMapping.forEach((packetTypeCode, targetValue) -> {
				RulPacketType packetType = ruleSystem.getPacketTypeByCode(packetTypeCode);
				objectMapping.put(packetType.getPacketTypeId(), targetValue);
			});
		}
	}

	public void apply(LevelWithItems level, UnitCountAction unitCountAction) {
        // check exclude condition
        if (excludeWhen != null) {
            if (excludeWhen.isTrue(level)) {
                // set as skip
                unitCountAction.setSkipSubtree(level);
                return;
            }
        }

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

				ArrPacket packet = ((ArrDataPacketRef) item.getData()).getPacket();
				Integer packetId = packet.getPacketId();
				if (!countedObjects.contains(packetId)) {
					//TODO: change to id getter - object not needed
					RulPacketType packetType = packet.getPacketType();
					if (packetType != null) {
						// find mapping
						String value = objectMapping.get(packetType.getPacketTypeId());
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
