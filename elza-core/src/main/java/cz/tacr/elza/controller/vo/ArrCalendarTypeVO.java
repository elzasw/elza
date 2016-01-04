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
    private Integer calendarTypeId;

    /**
     * Kod typu.
     */
    private String code;

    /**
     * Název typu.
     */
    private String name;

    public Integer getCalendarTypeId() {
        return calendarTypeId;
    }

    public void setCalendarTypeId(final Integer calendarTypeId) {
        this.calendarTypeId = calendarTypeId;
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
