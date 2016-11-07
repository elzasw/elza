package cz.tacr.elza.xmlimport.v1.vo.party;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;
import cz.tacr.elza.xmlimport.v1.vo.record.Record;

/**
 * Související entita se vztahem/událostí.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 13. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "role-type", namespace = NamespaceInfo.NAMESPACE)
public class RoleType {

    /** Typ role entity. */
    @XmlAttribute(name = "role-type-code")
    private String roleTypeCode;

    /** Rejstříkové heslo. */
    @XmlAttribute(name = "record-id", required = true)
    private String recordId;

    @XmlTransient
    private Record record;

    /** Zdroj informace. */
    @XmlElement(name = "source")
    private String source;

    public String getRoleTypeCode() {
        return roleTypeCode;
    }

    public void setRoleTypeCode(final String roleTypeCode) {
        this.roleTypeCode = roleTypeCode;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(final Record record) {
        this.record = record;
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
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
