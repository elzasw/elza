package cz.tacr.elza.xmlimport.v1.vo.party;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
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

    /** Id externího zdroje. */
    @XmlAttribute
    private String externId;

    /** Typ role entity. */
    @XmlAttribute
    private String roleTypeCode;

    /** Rejstříkové heslo. */
    @XmlAttribute(required = true)
    @XmlIDREF
    private Record record;

    public String getExternId() {
        return externId;
    }

    public void setExternId(String externId) {
        this.externId = externId;
    }

    public String getRoleTypeCode() {
        return roleTypeCode;
    }

    public void setRoleTypeCode(String roleTypeCode) {
        this.roleTypeCode = roleTypeCode;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
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
