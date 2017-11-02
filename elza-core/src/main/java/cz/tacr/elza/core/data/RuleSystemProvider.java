package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;

/**
 * Manage information about all rule systems
 * 
 *
 */
public class RuleSystemProvider {

	private List<RuleSystem> rulesSystems;

	private Map<Integer, RuleSystemImpl> ruleSetIdMap;

    private Map<String, RuleSystem> ruleSetCodeMap;

	/**
	 * Map of all item types
	 * 
	 * Key is ID of the Item type
	 */
	private Map<Integer, RuleSystemItemType> itemTypes;

    RuleSystemProvider() {
    }

    public RuleSystem getByRuleSetId(int id) {
        return ruleSetIdMap.get(id);
    }

    public RuleSystem getByRuleSetCode(String code) {
        Validate.notEmpty(code);
        return ruleSetCodeMap.get(code);
    }

	/**
	 * Get list of available rule systems
	 * 
	 * @return Return unmodifiable collection
	 */
	public List<RuleSystem> getRulesSystems() {
		return Collections.unmodifiableList(rulesSystems);
	}

    /**
     * Init all values. Method must be called inside transaction and synchronized.
     */
    void init(RuleSetRepository ruleSetRepository,
              PacketTypeRepository packetTypeRepository,
              ItemTypeRepository itemTypeRepository,
              ItemSpecRepository itemSpecRepository) {
        List<RulRuleSet> ruleSets = ruleSetRepository.findAll();

		// prepare fields
		List<RuleSystemImpl> rulesSystemsImpl = new ArrayList<>(ruleSets.size());
		ruleSetIdMap = new HashMap<>(ruleSets.size());
		ruleSetCodeMap = new HashMap<>(ruleSets.size());

        for (RulRuleSet rs : ruleSets) {
            // create initialized rule system
			RuleSystemImpl ruleSystem = new RuleSystemImpl(rs);

			rulesSystemsImpl.add(ruleSystem);
            // update lookups
			ruleSetIdMap.put(rs.getRuleSetId(), ruleSystem);
			ruleSetCodeMap.put(rs.getCode(), ruleSystem);
        }

		// prepare packet types
		initPacketTypes(packetTypeRepository);

		// prepare item types
		initItemTypes(itemTypeRepository, itemSpecRepository);

		// seal up all created rule systems
		rulesSystems = rulesSystemsImpl.stream().map(a -> a.sealUp()).collect(Collectors.toList());
    }

	private void initPacketTypes(PacketTypeRepository packetTypeRepository) {
		List<RulPacketType> packetTypes = packetTypeRepository.findAll();

		for (RulPacketType pt : packetTypes) {
			RuleSystemImpl ruleSetImpl = ruleSetIdMap.get(pt.getRuleSet().getRuleSetId());
			ruleSetImpl.addPacketType(pt);
		}
	}

	/**
	 * Initialize all item types
	 * 
	 * @param itemTypeRepository
	 * @param itemSpecRepository
	 */
	private void initItemTypes(ItemTypeRepository itemTypeRepository, ItemSpecRepository itemSpecRepository) {
		List<RulItemType> itemTypes = itemTypeRepository.findAll();
		//.findByRulPackage(ruleSet.getPackage());

		this.itemTypes = new HashMap<>(itemTypes.size());
		for (RulItemType it : itemTypes) {
			// update data type reference from cache
			DataType dataType = DataType.fromId(it.getDataTypeId());
			it.setDataType(dataType.getEntity());

			// Find rule system
			RuleSystemImpl ruleSetImpl = ruleSetIdMap.get(it.getRuleSet().getRuleSetId());

			// create initialized rule system item type
			RuleSystemItemType rsit = new RuleSystemItemType(ruleSetImpl, it, dataType);
			rsit.init(itemSpecRepository);

			ruleSetImpl.addItemType(rsit);

			this.itemTypes.put(it.getItemTypeId(), rsit);
		}
	}

	/**
	 * Get item type by id
	 * 
	 * @param itemTypeId
	 * @return
	 */
	public RuleSystemItemType getItemType(Integer itemTypeId) {
		return itemTypes.get(itemTypeId);
	}
}
