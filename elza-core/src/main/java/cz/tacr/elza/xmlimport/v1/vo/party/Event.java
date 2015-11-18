package cz.tacr.elza.xmlimport.v1.vo.party;

import java.util.Date;
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

/**
 * Vztah a událost.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 13. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "event", namespace = NamespaceInfo.NAMESPACE)
public class Event {

    /** Třída vztahu. */
    @XmlAttribute
    private String classTypeCode;

    /** Typ vztahu. */
    @XmlAttribute
    private String relationTypeCode;

    /** Datum počátku. */
    @XmlElement
    private Date fromDate;

    /** Datum konce. */
    @XmlElement
    private Date toDate;

    /** Zdroj informace. */
    @XmlElement
    private String source;

    /** Zdroj informace. */
    @XmlElement
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

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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