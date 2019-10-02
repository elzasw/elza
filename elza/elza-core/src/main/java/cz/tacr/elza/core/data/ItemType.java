package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;

public class ItemType {

    private final RulItemType itemType;

    private final DataType dataType;

    private List<RulItemSpec> itemSpecs;

    private Map<Integer, RulItemSpec> itemSpecIdMap;

    private Map<String, RulItemSpec> itemSpecCodeMap;

    ItemType(RulItemType itemType, DataType dataType) {
        this.itemType = itemType;
        this.dataType = dataType;

        this.itemSpecs = new ArrayList<>();

        // ensure reference equality
        Validate.isTrue(itemType.getDataType() == dataType.getEntity());
    }

    public DataType getDataType() {
        return dataType;
    }

    public RulItemType getEntity() {
        return itemType;
    }

    public Integer getDataTypeId() {
        return itemType.getDataTypeId();
    }

    public Integer getItemTypeId() {
        return itemType.getItemTypeId();
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

    public RulItemSpec getItemSpecById(Integer id) {
        Validate.notNull(id);
        return itemSpecIdMap.get(id);
    }

    public RulItemSpec getItemSpecByCode(String code) {
        Validate.notEmpty(code);
        return itemSpecCodeMap.get(code);
    }

    void addItemSpec(RulItemSpec is) {
        itemSpecs.add(is);
    }

    void sealUp() {
        // update fields
        this.itemSpecIdMap = StaticDataProvider.createLookup(itemSpecs, RulItemSpec::getItemSpecId);
        this.itemSpecCodeMap = StaticDataProvider.createLookup(itemSpecs, RulItemSpec::getCode);

        // switch to unmodifiable collections
        itemSpecs = Collections.unmodifiableList(itemSpecs);
    }
}
