package cz.tacr.elza.xmlimport.v1.vo.party;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;
import cz.tacr.elza.xmlimport.v1.vo.date.ComplexDate;

/**
 * Působnost osoby.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "party-time-range", namespace = NamespaceInfo.NAMESPACE)
public class PartyTimeRange {

    /** Od. */
    @XmlElement(name = "from-date", required = true)
    private ComplexDate fromDate;

    /** Do. */
    @XmlElement(name = "to-date")
    private ComplexDate toDate;

    public ComplexDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(ComplexDate fromDate) {
        this.fromDate = fromDate;
    }

    public ComplexDate getToDate() {
        return toDate;
    }

    public void setToDate(ComplexDate toDate) {
        this.toDate = toDate;
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
