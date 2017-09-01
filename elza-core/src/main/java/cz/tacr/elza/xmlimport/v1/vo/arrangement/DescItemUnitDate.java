package cz.tacr.elza.xmlimport.v1.vo.arrangement;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Datace.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "desc-item-unit-date", namespace = NamespaceInfo.NAMESPACE)
public class DescItemUnitDate extends AbstractDescItem {

    @XmlElement(name = "value-from")
    private String valueFrom;

    @XmlElement(name = "value-from-estimated")
    private Boolean valueFromEstimated;

    @XmlElement(name = "value-to")
    private String valueTo;

    @XmlElement(name = "value-to-estimated")
    private Boolean valueToEstimated;

    @XmlAttribute(name = "calendar-type-code")
    private String calendarTypeCode;

    @XmlElement(name = "format")
    private String format;

    public String getValueFrom() {
        return valueFrom;
    }

    public void setValueFrom(String valueFrom) {
        this.valueFrom = valueFrom;
    }

    public Boolean getValueFromEstimated() {
        return valueFromEstimated;
    }

    public void setValueFromEstimated(Boolean valueFromEstimated) {
        this.valueFromEstimated = valueFromEstimated;
    }

    public String getValueTo() {
        return valueTo;
    }

    public void setValueTo(String valueTo) {
        this.valueTo = valueTo;
    }

    public Boolean getValueToEstimated() {
        return valueToEstimated;
    }

    public void setValueToEstimated(Boolean valueToEstimated) {
        this.valueToEstimated = valueToEstimated;
    }

    public String getCalendarTypeCode() {
        return calendarTypeCode;
    }

    public void setCalendarTypeCode(String calendarTypeCode) {
        this.calendarTypeCode = calendarTypeCode;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
