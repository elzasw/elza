package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * VO PolicyTypes.
 *
 * @author Martin Šlapa
 * @since 22.3.2016
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "policy-types")
@XmlType(name = "policy-types")
public class PolicyTypes {

    @XmlElement(name = "policy-type", required = true)
    private List<PolicyType> policyTypes;

    public List<PolicyType> getPolicyTypes() {
        return policyTypes;
    }

    public void setPolicyTypes(final List<PolicyType> policyTypes) {
        this.policyTypes = policyTypes;
    }
}
