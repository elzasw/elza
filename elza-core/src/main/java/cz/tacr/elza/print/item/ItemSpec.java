package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.RulItemSpec;

/**
 * Item specification for output
 */
public class ItemSpec {

    private final String name;

    private final String shortcut;

    private final String description;

    private final String code;

    public ItemSpec(RulItemSpec rulItemSpec) {
        this.name = rulItemSpec.getName();
        this.shortcut = rulItemSpec.getShortcut();
        this.description = rulItemSpec.getDescription();
        this.code = rulItemSpec.getCode();
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getShortcut() {
        return shortcut;
    }
}
