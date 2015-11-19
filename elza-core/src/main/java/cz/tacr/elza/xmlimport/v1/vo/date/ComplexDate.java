package cz.tacr.elza.xmlimport.v1.vo.date;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Datum.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 19. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "complex-date", namespace = NamespaceInfo.NAMESPACE)
public class ComplexDate {

    /** Konkrétní datum. */
    @XmlElement(name = "specific-date")
    private Date specificDate;

    /** Datum zadané textem. */
    @XmlElement(name = "text-date")
    private String textDate;

    /** Konkrétní datum, rozmezí od. */
    @XmlElement(name = "specific-date-from")
    private Date specificDateFrom;

    /** Konkrétní datum, rozmezí do. */
    @XmlElement(name = "specific-date-to")
    private Date specificDateTo;

    public Date getSpecificDate() {
        return specificDate;
    }

    public void setSpecificDate(Date specificDate) {
        this.specificDate = specificDate;
    }

    public String getTextDate() {
        return textDate;
    }

    public void setTextDate(String textDate) {
        this.textDate = textDate;
    }

    public Date getSpecificDateFrom() {
        return specificDateFrom;
    }

    public void setSpecificDateFrom(Date specificDateFrom) {
        this.specificDateFrom = specificDateFrom;
    }

    public Date getSpecificDateTo() {
        return specificDateTo;
    }

    public void setSpecificDateTo(Date specificDateTo) {
        this.specificDateTo = specificDateTo;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
