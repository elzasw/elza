package cz.tacr.elza.controller.vo;

/**
 * Číselník typů kalendářů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
public class ArrCalendarTypeVO {

    /**
     * Id typu kalendáře.
     */
    private Integer id;

    /**
     * Kod typu.
     */
    private String code;

    /**
     * Název typu.
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
