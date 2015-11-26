package cz.tacr.elza.xmlimport.v1.vo.party;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;
import cz.tacr.elza.xmlimport.v1.vo.date.ComplexDate;

/**
 * Vztah a událost.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 13. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "relation", namespace = NamespaceInfo.NAMESPACE)
public class Relation {

    /** Třída vztahu. */
    @XmlAttribute(name = "class-type-code")
    private String classTypeCode;

    /** Typ vztahu. */
    @XmlAttribute(name = "relation-type-code")
    private String relationTypeCode;

    /** Datum počátku. */
    @XmlElement(name = "from-date")
    private ComplexDate fromDate;

    /** Datum konce. */
    @XmlElement(name = "to-date")
    private ComplexDate toDate;

    /** Poznámka k dataci. */
    @XmlElement(name = "date-note")
    private String dateNote;

    /** Zdroj informace. */
    @XmlElement(name = "note")
    private String note;

    /** Typy rolí. */
    @XmlElement(name = "role-type")
    @XmlElementWrapper(name = "role-type-list")
    private List<RoleType> roleTypes;

    public String getClassTypeCode() {
        return classTypeCode;
    }

    public void setClassTypeCode(String classTypeCode) {
        this.classTypeCode = classTypeCode;
    }

    public String getRelationTypeCode() {
        return relationTypeCode;
    }

    public void setRelationTypeCode(String relationTypeCode) {
        this.relationTypeCode = relationTypeCode;
    }

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

    public String getDateNote() {
        return dateNote;
    }

    public void setDateNote(String dateNote) {
        this.dateNote = dateNote;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<RoleType> getRoleTypes() {
        return roleTypes;
    }

    public void setRoleTypes(List<RoleType> roleTypes) {
        this.roleTypes = roleTypes;
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