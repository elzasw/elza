package cz.tacr.elza.controller.vo;

import cz.tacr.elza.bulkaction.BulkActionConfig;

/**
 * Hromadné akce
 *
 * @author Petr Compel [petr.compel@marbes.cz]
 * @since 29. 1. 2016
 */
public class BulkActionVO {

    private String name;

    private String code;

    private String description;

    private boolean fastAction;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isFastAction() {
        return fastAction;
    }

    public void setFastAction(boolean fastAction) {
        this.fastAction = fastAction;
    }
    
    public static BulkActionVO newInstance(final BulkActionConfig bulkAction) {
    	BulkActionVO result = new BulkActionVO();
    	result.setName(bulkAction.getName());
    	result.setCode(bulkAction.getCode());
    	result.setDescription(bulkAction.getDescription());
    	return result;
    }
}
