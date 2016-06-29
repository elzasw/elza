package cz.tacr.elza.controller.vo;

/**
 * @author Pavel Stánek
 * @since 29.06.2016
 */
public class RulTemplateVO {
    /** Kód. */
    private String code;
    /** N8yev. */
    private String name;
    /** Typ systému - enum převedený na string. */
    private String engine;
    /** Adresář pro výstupy. */
    private String directory;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

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
