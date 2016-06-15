package cz.tacr.elza.controller.vo;

/**
 * VO skupiny.
 *
 * @author Pavel Stánek
 * @since 15.06.2016
 */
public class GroupVO {
    /** Identifikátor. */
    private Integer id;
    /** Kód. */
    private String code;
    /** Název. */
    private String name;
    /** Popis. */
    private String description;

    /** Identifikátor. */
    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    /** Kód. */
    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    /** Název. */
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /** Popis. */
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
