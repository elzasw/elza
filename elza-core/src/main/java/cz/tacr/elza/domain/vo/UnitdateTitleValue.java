package cz.tacr.elza.domain.vo;

/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 12.04.2016
 */
public class UnitdateTitleValue extends TitleValue {

    private Integer calendarTypeId;

    public UnitdateTitleValue() {

    }

    public UnitdateTitleValue(final String value, final Integer calendarTypeId) {
        super(value);
        this.calendarTypeId = calendarTypeId;
    }

    public Integer getCalendarTypeId() {
        return calendarTypeId;
    }

    public void setCalendarTypeId(final Integer calendarTypeId) {
        this.calendarTypeId = calendarTypeId;
    }
}
