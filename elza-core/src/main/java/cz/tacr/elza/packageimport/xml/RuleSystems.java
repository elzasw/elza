package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;
import java.util.List;


/**
 * VO RuleSystems.
 *
 * @since 01.11.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rule-systems")
@XmlType(name = "rule-systems")
public class RuleSystems {

    @XmlElement(name = "rule-system", required = true)
    private List<RuleSystem> ruleSystems;

    public List<RuleSystem> getRuleSystems() {
        return ruleSystems;
    }

    public void setRuleSystems(final List<RuleSystem> ruleSystems) {
        this.ruleSystems = ruleSystems;
    }
}
