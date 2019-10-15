package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * VO typ vztahu.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "registry-role")
public class RegistryRole {

    @XmlAttribute(name = "register-type", required = true)
    private String registerType;

    @XmlAttribute(name = "role-type", required = true)
    private String roleType;

    public String getRegisterType() {
        return registerType;
    }

    public void setRegisterType(final String registerType) {
        this.registerType = registerType;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(final String roleType) {
        this.roleType = roleType;
    }
}
