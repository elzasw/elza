package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.generator.multiple.Action;
import cz.tacr.elza.bulkaction.generator.multiple.ActionConfig;
import cz.tacr.elza.bulkaction.generator.multiple.DeleteItemAction;

/**
 * Nastavení akce {@link cz.tacr.elza.bulkaction.generator.multiple.DeleteItemAction}
 */
public class DeleteItemConfig implements ActionConfig {

    private String inputType;

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    @Override
    public Class<? extends Action> getActionClass() {
        return DeleteItemAction.class;
    }
}
