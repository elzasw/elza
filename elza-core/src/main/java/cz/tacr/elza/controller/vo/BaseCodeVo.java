package cz.tacr.elza.controller.vo;

/**
 * Base value object with:
 * - id
 * - name
 * - code
 *
 */
public abstract class BaseCodeVo {

    /**
     * identifikátor
     */
    private Integer id;

    /**
     * kód
     */
    private String code;

    /**
     * název
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
