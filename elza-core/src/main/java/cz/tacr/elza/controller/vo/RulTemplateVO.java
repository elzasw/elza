package cz.tacr.elza.controller.vo;

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
}
