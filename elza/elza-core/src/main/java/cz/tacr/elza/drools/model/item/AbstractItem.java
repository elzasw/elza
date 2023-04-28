package cz.tacr.elza.drools.model.item;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.RulItemSpec;

public abstract class AbstractItem {

    private Integer id;
    private ItemType itemType;
    private RulItemSpec spec;

    AbstractItem(final Integer id, final ItemType itemType, final RulItemSpec itemSpec) {
        Validate.notNull(itemType);

        this.id = id;
        this.itemType = itemType;
        this.spec = itemSpec;
    }

    public Integer getId() {
        return id;
    }

    public String getType() {
        return itemType.getCode();
    }

    public ItemType getItemType() {
        return itemType;
    }

    public String getSpec() {
        if (spec == null) {
            return null;
        } else {
            return spec.getCode();
        }
    }

    // TODO: Method is probably never used and could be removed
    //       We have to check rules
    public String getDataType() {
        return itemType.getDataType().getCode();
    }
}
