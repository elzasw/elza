package cz.tacr.elza.controller.vo;

/**
 * Třída pro externí identifikátor.
 *
 * @since 27.07.2018
 */
public class ApEidTypeVO {

    /**
     * Identifikátor typu.
     */
    private Integer id;

    /**
     * Kód.
     */
    private String code;

    /**
     * Název.
     */
    private String name;

    public ApEidTypeVO() {
    }

    public ApEidTypeVO(final Integer id, final String code, final String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

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
