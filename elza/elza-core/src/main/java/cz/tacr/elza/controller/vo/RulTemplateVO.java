package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.RulTemplate;

/**
 */
public class RulTemplateVO extends BaseCodeVo {

    /** Typ systému - enum převedený na string. */
    private String engine;
    /** Adresář pro výstupy. */
    private String directory;

    public String getEngine() {
        return engine;
    }

    public void setEngine(final String engine) {
        this.engine = engine;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(final String directory) {
        this.directory = directory;
    }

    public static RulTemplateVO newInstance(final RulTemplate rulTemplate) {
    	RulTemplateVO result = new RulTemplateVO();
    	result.setId(rulTemplate.getTemplateId());
    	result.setCode(rulTemplate.getCode());
    	result.setDirectory(rulTemplate.getDirectory());
    	result.setEngine(rulTemplate.getEngine().toString());
    	result.setName(rulTemplate.getName());
    	return result;
    }
}
