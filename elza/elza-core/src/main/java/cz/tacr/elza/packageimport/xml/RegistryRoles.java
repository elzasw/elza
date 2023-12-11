package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;


/**
 * VO vztah typu třídy - seznam.
 *
 * @author Martin Šlapa
 * @since 21.11.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "registry-roles")
@XmlType(name = "registry-roles")
public class RegistryRoles {

    @XmlElement(name = "registry-role", required = true)
    private List<RegistryRole> registryRoles;

    public List<RegistryRole> getRegistryRoles() {
        return registryRoles;
    }

    public void setRegistryRoles(final List<RegistryRole> registryRoles) {
        this.registryRoles = registryRoles;
    }
}
