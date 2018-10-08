package cz.tacr.elza.controller;

/**
 * VO pro rozšíření AS.
 *
 * @since 11.11.2017
 */
public class StructureExtensionFundVO {

    private Integer id;

    private String code;

    private String name;

    private Boolean active;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}
