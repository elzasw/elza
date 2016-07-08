package cz.tacr.elza.controller.vo.nodes.descitems;

/**
 * VO hodnoty atributu - unit date.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemUnitdateVO extends ArrItemVO {

    /**
     * hodnota
     */
    private String value;

    /**
     * identifikátor kalendáře
     */
    private Integer calendarTypeId;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public Integer getCalendarTypeId() {
        return calendarTypeId;
    }

    public void setCalendarTypeId(final Integer calendarTypeId) {
        this.calendarTypeId = calendarTypeId;
    }
}