package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;
import java.util.List;


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
