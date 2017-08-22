package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.util.Assert;

import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.PacketTypeRepository;

public class RuleSystem {

    private final RulRuleSet ruleSet;

    private List<RulPacketType> packetTypes;

    private List<RuleSystemItemType> itemTypes;

    private Map<String, RulPacketType> packetTypeCodeMap;

    private Map<Integer, RuleSystemItemType> itemTypeIdMap;

    private Map<String, RuleSystemItemType> itemTypeCodeMap;

    RuleSystem(RulRuleSet ruleSet) {
        this.ruleSet = Objects.requireNonNull(ruleSet);
    }

    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    public List<RulPacketType> getPacketTypes() {
        return packetTypes;
    }

    public RulPacketType getPacketTypeByCode(String code) {
        Assert.hasLength(code);
        return packetTypeCodeMap.get(code);
    }

    public List<RuleSystemItemType> getItemTypes() {
        return itemTypes;
    }

    public RuleSystemItemType getItemTypeById(Integer id) {
        Assert.notNull(id);
        return itemTypeIdMap.get(id);
    }

    public RuleSystemItemType getItemTypeByCode(String code) {
        Assert.hasLength(code);
        return itemTypeCodeMap.get(code);
    }

    /**
     * Init all values. Method must be called inside transaction and synchronized.
     */
    void init(PacketTypeRepository packetTypeRepository,
              ItemTypeRepository itemTypeRepository,
              ItemSpecRepository itemSpecRepository) {
        initPacketTypes(packetTypeRepository);
        initItemTypes(itemTypeRepository, itemSpecRepository);
    }

    private void initPacketTypes(PacketTypeRepository packetTypeRepository) {
        List<RulPacketType> packetTypes = packetTypeRepository.findByRulPackage(ruleSet.getPackage());

        // ensure reference equality
        for (RulPacketType pt : packetTypes) {
            Assert.isTrue(ruleSet.getPackage() == pt.getPackage());
        }
        // update fields
        this.packetTypes = Collections.unmodifiableList(packetTypes);
        this.packetTypeCodeMap = StaticDataProvider.createLookup(packetTypes, RulPacketType::getCode);
    }

    private void initItemTypes(ItemTypeRepository itemTypeRepository, ItemSpecRepository itemSpecRepository) {
        List<RulItemType> itemTypes = itemTypeRepository.findByRulPackage(ruleSet.getPackage());

        List<RuleSystemItemType> rsItemTypes = new ArrayList<>(itemTypes.size());
        for (RulItemType it : itemTypes) {
            // update data type reference from cache
            DataType dataType = DataType.fromId(it.getDataTypeId());
            it.setDataType(dataType.getEntity());

            // create initialized rule system item type
            RuleSystemItemType rsit = new RuleSystemItemType(this, it, dataType);
            rsit.init(itemSpecRepository);
            rsItemTypes.add(rsit);
        }
        // update fields
        this.itemTypes = Collections.unmodifiableList(rsItemTypes);
        this.itemTypeIdMap = StaticDataProvider.createLookup(rsItemTypes, RuleSystemItemType::getId);
        this.itemTypeCodeMap = StaticDataProvider.createLookup(rsItemTypes, RuleSystemItemType::getCode);
    }
}
