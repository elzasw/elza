package cz.tacr.elza.controller.vo;

/**
 * Třída pro jazyk.
 *
 * @since 11.07.2018
 */
public class LanguageVO {

    /**
     * Identifikátor jazyku.
     */
    private Integer id;

    /**
     * Jedinečný kód jazyku - 3 místný.
     */
    private String code;

    /**
     * Název jazyku.
     */
    private String name;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

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
}
