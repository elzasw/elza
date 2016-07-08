package cz.tacr.elza.packageimport.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * VO RuleSets.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rule-sets")
@XmlType(name = "rule-sets")
public class RuleSets {

    @XmlElement(name = "rule-set", required = true)
    private List<RuleSet> ruleSets;

    public List<RuleSet> getRuleSets() {
        return ruleSets;
    }

    public void setRuleSets(final List<RuleSet> ruleSets) {
        this.ruleSets = ruleSets;
    }
}
