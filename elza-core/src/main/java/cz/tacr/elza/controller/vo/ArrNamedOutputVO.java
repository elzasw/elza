package cz.tacr.elza.controller.vo;

/**
 * VO Výstup z archivního souboru.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.04.2016
 */
public class ArrNamedOutputVO {

    private Integer id;

    private String code;

    private String name;

    private Boolean temporary;

    private Boolean deleted;

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

    public Boolean getTemporary() {
        return temporary;
    }

    public void setTemporary(final Boolean temporary) {
        this.temporary = temporary;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }
}
