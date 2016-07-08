package cz.tacr.elza.domain.vo;

/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 12.04.2016
 */
public class UnitdateDescItemValue extends DescItemValue {

    private Integer calendarTypeId;

    public UnitdateDescItemValue() {

    }

    public UnitdateDescItemValue(final String value, final String specCode,
                                 final Integer calendarTypeId) {
        super(value, specCode);
        this.calendarTypeId = calendarTypeId;
    }

    public Integer getCalendarTypeId() {
        return calendarTypeId;
    }

    public void setCalendarTypeId(final Integer calendarTypeId) {
        this.calendarTypeId = calendarTypeId;
    }
}
