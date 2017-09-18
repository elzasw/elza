package cz.tacr.elza.core.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.repository.ItemSpecRepository;

public class RuleSystemItemType {

    private final RuleSystem ruleSystem;

    private final RulItemType itemType;

    private final DataType dataType;

    private List<RulItemSpec> itemSpecs;

    private Map<String, RulItemSpec> itemSpecCodeMap;

    RuleSystemItemType(RuleSystem ruleSystem, RulItemType itemType, DataType dataType) {
        this.ruleSystem = ruleSystem;
        this.itemType = itemType;
        this.dataType = dataType;

        // ensure reference equality
        Validate.isTrue(ruleSystem.getRuleSet().getPackage() == itemType.getPackage());
        Validate.isTrue(itemType.getDataType() == dataType.getEntity());
    }

    public RuleSystem getRuleSystem() {
        return ruleSystem;
    }

    public DataType getDataType() {
        return dataType;
    }

    public RulItemType getEntity() {
        return itemType;
    }

    public Integer getId() {
        return itemType.getDataTypeId();
    }

    public String getCode() {
        return itemType.getCode();
    }

    public boolean hasSpecifications() {
        return itemType.getUseSpecification();
    }

    public List<RulItemSpec> getItemSpecs() {
        return itemSpecs;
    }

    public RulItemSpec getItemSpecByCode(String code) {
        Validate.notEmpty(code);
        return itemSpecCodeMap.get(code);
    }

    /**
     * Init all values. Method must be called inside transaction and synchronized.
     */
    void init(ItemSpecRepository itemSpecRepository) {
        List<RulItemSpec> itemSpecs = Collections.emptyList();
        Map<String, RulItemSpec> codeMap = Collections.emptyMap();

        if (hasSpecifications()) {
            itemSpecs = itemSpecRepository.findByItemType(itemType);
            itemSpecs = Collections.unmodifiableList(itemSpecs);
            codeMap = StaticDataProvider.createLookup(itemSpecs, RulItemSpec::getCode);

            // ensure reference equality
            for (RulItemSpec is : itemSpecs) {
                Validate.isTrue(itemType == is.getItemType());
                Validate.isTrue(itemType.getPackage() == is.getPackage());
            }
        }
        // update fields
        this.itemSpecs = itemSpecs;
        this.itemSpecCodeMap = codeMap;
    }
}
