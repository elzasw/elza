package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PacketTypeRepository;
import cz.tacr.elza.repository.RuleSetRepository;

/**
 * Manage information about all rule systems
 */
public class RuleSystemProvider {

    private List<RuleSystem> rulesSystems;

    private Map<Integer, RuleSystemImpl> ruleSystemIdMap;

    private Map<String, RuleSystem> ruleSystemCodeMap;

    /**
     * Map of all item types
     *
     * Key is ID of the Item type
     */
    private Map<Integer, RuleSystemItemType> itemTypeIdMap;

    private Map<Integer, RulItemSpec> itemSpecIdMap;

    RuleSystemProvider() {
    }

    public RuleSystem getByRuleSetId(int id) {
        return ruleSystemIdMap.get(id);
    }

    public RuleSystem getByRuleSetCode(String code) {
        Validate.notEmpty(code);
        return ruleSystemCodeMap.get(code);
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
     * Get item type by id
     *
     * @param itemTypeId
     * @return
     */
    public RuleSystemItemType getItemType(Integer itemTypeId) {
        Validate.notNull(itemTypeId);
        return itemTypeIdMap.get(itemTypeId);
    }

    public RulItemSpec getItemSpec(Integer itemSpecId) {
        Validate.notNull(itemSpecId);
        return itemSpecIdMap.get(itemSpecId);
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
        ruleSystemIdMap = new HashMap<>(ruleSets.size());
        ruleSystemCodeMap = new HashMap<>(ruleSets.size());

        for (RulRuleSet rs : ruleSets) {
            // create initialized rule system
            RuleSystemImpl ruleSystem = new RuleSystemImpl(rs);

            rulesSystemsImpl.add(ruleSystem);
            // update lookups
            ruleSystemIdMap.put(rs.getRuleSetId(), ruleSystem);
            ruleSystemCodeMap.put(rs.getCode(), ruleSystem);
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
            RuleSystemImpl ruleSystemImpl = ruleSystemIdMap.get(pt.getRuleSet().getRuleSetId());
            ruleSystemImpl.addPacketType(pt);
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

        itemTypeIdMap = new HashMap<>(itemTypes.size());
        itemSpecIdMap = new HashMap<>();

        for (RulItemType it : itemTypes) {
            // update data type reference from cache
            DataType dataType = DataType.fromId(it.getDataTypeId());
            it.setDataType(dataType.getEntity());

            // find rule system
            RuleSystemImpl ruleSystemImpl = ruleSystemIdMap.get(it.getRuleSet().getRuleSetId());

            // create initialized rule system item type
            RuleSystemItemType rsit = new RuleSystemItemType(ruleSystemImpl, it, dataType);

            // prepare item spec
            initItemSpecs(rsit, itemSpecRepository);

            rsit.sealUp();

            ruleSystemImpl.addItemType(rsit);

            itemTypeIdMap.put(it.getItemTypeId(), rsit);
        }
    }

    private void initItemSpecs(RuleSystemItemType rsit, ItemSpecRepository itemSpecRepository) {
        if (!rsit.hasSpecifications()) {
            return;
        }

        List<RulItemSpec> itemSpecs = itemSpecRepository.findByItemType(rsit.getEntity());
        for (RulItemSpec is : itemSpecs) {
            // check if initialized in same transaction
            Validate.isTrue(rsit.getEntity() == is.getItemType());

            rsit.addItemSpec(is);

            itemSpecIdMap.put(is.getItemSpecId(), is);
        }
    }
}
