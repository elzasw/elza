package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.RulItemSpec;

/**
 * Item specification for output
 */
public class ItemSpec {

    public String name;
    public String shortcut;
    public String description;
    public String code;

    public ItemSpec(RulItemSpec rulItemSpec) {
        name = rulItemSpec.getName();
        shortcut = rulItemSpec.getShortcut();
        description = rulItemSpec.getDescription();
        code = rulItemSpec.getCode();
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

    public static ItemSpec instanceOf(final RulItemSpec rulItemSpec) {
    	return new ItemSpec(rulItemSpec); 
    }
}
