package cz.tacr.elza.core.data;

import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RuleSystem {

    private final RulRuleSet ruleSet;

    private List<RuleSystemItemType> itemTypes;

    private Map<Integer, RuleSystemItemType> itemTypeIdMap;

    private Map<String, RuleSystemItemType> itemTypeCodeMap;

    RuleSystem(RulRuleSet ruleSet) {
        this.ruleSet = Validate.notNull(ruleSet);
    }

    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    public List<RuleSystemItemType> getItemTypes() {
        return itemTypes;
    }

    public RuleSystemItemType getItemTypeById(Integer id) {
        Validate.notNull(id);
        return itemTypeIdMap.get(id);
    }

    public RuleSystemItemType getItemTypeByCode(String code) {
        Validate.notEmpty(code);
        return itemTypeCodeMap.get(code);
    }

    /**
     * Init all values. Method must be called inside transaction and synchronized.
     */
    void init(ItemTypeRepository itemTypeRepository,
              ItemSpecRepository itemSpecRepository) {
        initItemTypes(itemTypeRepository, itemSpecRepository);
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
