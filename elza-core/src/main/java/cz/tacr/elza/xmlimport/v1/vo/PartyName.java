package cz.tacr.elza.xmlimport.v1.vo;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Preferované jméno osoby.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "party-name", namespace = NamespaceInfo.NAMESPACE)
public class PartyName {

    @XmlElement
    private String mainPart;

    @XmlElement
    private String otherPart;

    @XmlElement
    private String anotation;

    @XmlElement
    private String degreeBefore;

    @XmlElement
    private String degreeAfter;

    @XmlSchemaType(name="dateTime", type = Date.class)
    @XmlElement
    private Date validFrom;

    @XmlSchemaType(name="dateTime", type = Date.class)
    @XmlElement
    private Date validTo;

    public String getMainPart() {
        return mainPart;
    }

    public void setMainPart(String mainPart) {
        this.mainPart = mainPart;
    }

    public String getOtherPart() {
        return otherPart;
    }

    public void setOtherPart(String otherPart) {
        this.otherPart = otherPart;
    }

    public String getAnotation() {
        return anotation;
    }

    public void setAnotation(String anotation) {
        this.anotation = anotation;
    }

    public String getDegreeBefore() {
        return degreeBefore;
    }

    public void setDegreeBefore(String degreeBefore) {
        this.degreeBefore = degreeBefore;
    }

    public String getDegreeAfter() {
        return degreeAfter;
    }

    public void setDegreeAfter(String degreeAfter) {
        this.degreeAfter = degreeAfter;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
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
