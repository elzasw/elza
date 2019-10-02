package cz.tacr.elza.service.vo;

/**
 * Modelový objekt pro groovy - jazyk.
 */
public class Language {

    /**
     * Identifikátor.
     */
    private Integer id;

    /**
     * Název jazyku.
     */
    private String name;

    /**
     * Kód jazyku.
     */
    private String code;

    public Language(final Integer id, final String name, final String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }
}
