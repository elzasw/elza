package cz.tacr.elza.drools.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.RulItemSpec;

public class ItemType {

    final private cz.tacr.elza.core.data.ItemType itemType;

    private boolean repeatable;
    private RequiredType requiredType;
    private List<ItemSpec> specs;
    private Map<String, ItemSpec> specMap;

    public ItemType(final cz.tacr.elza.core.data.ItemType itemType) {
        this.itemType = itemType;
        this.repeatable = false;
        this.requiredType = RequiredType.IMPOSSIBLE;
        
        List<RulItemSpec> itemSpecs = itemType.getItemSpecs();
        if (CollectionUtils.isNotEmpty(itemType.getItemSpecs())) {
            specs = new ArrayList<>(itemSpecs.size());
            specMap = new HashMap<>();
            for (RulItemSpec itemSpec : itemSpecs) {
                ItemSpec spec = new ItemSpec(itemSpec);
                specs.add(spec);
                specMap.put(itemSpec.getCode(), spec);
            }
        } else {
            specs = Collections.emptyList();
            specMap = Collections.emptyMap();
        }
    }

    public String getCode() {
        return itemType.getCode();
    }

    public cz.tacr.elza.core.data.ItemType getItemType() {
        return itemType;
    }

    public DataType getDataType() {
        return itemType.getDataType();
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final boolean repeatable) {
        this.repeatable = repeatable;
    }

    public RequiredType getRequiredType() {
        return requiredType;
    }

    public void setRequired() {
        requiredType = RequiredType.REQUIRED;
    }

    public void setPossible() {
        requiredType = RequiredType.POSSIBLE;
    }

    public void setImpossible() {
        requiredType = RequiredType.IMPOSSIBLE;
    }

    public void setRequiredType(final RequiredType requiredType) {
        this.requiredType = requiredType;
    }

    public List<ItemSpec> getSpecs() {
        return specs;
    }

    public ItemSpec getSpec(String specCode) {
        if (specCode == null) {
            return null;
        }
        return specMap.get(specCode);
    }
}
