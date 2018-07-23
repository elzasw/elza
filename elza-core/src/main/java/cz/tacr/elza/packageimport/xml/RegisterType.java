package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO typ vztahu.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "register-type")
public class RegisterType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "party-type")
    private String partyType;

    @XmlAttribute(name = "parent-register-type")
    private String parentRegisterType;

    @XmlAttribute(name = "rule-system")
    private String ruleSystem;

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "read-only", required = true)
    private boolean readOnly;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getPartyType() {
        return partyType;
    }

    public void setPartyType(final String partyType) {
        this.partyType = partyType;
    }

    public String getParentRegisterType() {
        return parentRegisterType;
    }

    public void setParentRegisterType(final String parentRegisterType) {
        this.parentRegisterType = parentRegisterType;
    }

    public String getRuleSystem() {
        return ruleSystem;
    }

    public void setRuleSystem(final String ruleSystem) {
        this.ruleSystem = ruleSystem;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }
}
