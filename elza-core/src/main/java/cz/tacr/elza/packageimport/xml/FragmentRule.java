package cz.tacr.elza.packageimport.xml;

import cz.tacr.elza.domain.ApFragmentRule;

import javax.xml.bind.annotation.*;

/**
 * VO FragmentRule.
 *
 * @since 18.07.2018
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fragment-rule")
public class FragmentRule {

    @XmlAttribute(name = "filename", required = true)
    private String filename;

    @XmlElement(name = "rule-type", required = true)
    private ApFragmentRule.RuleType ruleType;

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public ApFragmentRule.RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(final ApFragmentRule.RuleType ruleType) {
        this.ruleType = ruleType;
    }
}
