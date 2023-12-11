package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


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
    private List<RuleSetXml> ruleSets;

    public List<RuleSetXml> getRuleSets() {
        return ruleSets;
    }

    public void setRuleSets(final List<RuleSetXml> ruleSets) {
        this.ruleSets = ruleSets;
    }
}
