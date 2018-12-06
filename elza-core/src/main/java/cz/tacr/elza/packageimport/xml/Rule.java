package cz.tacr.elza.packageimport.xml;

import cz.tacr.elza.domain.ApRule;

import javax.xml.bind.annotation.*;

/**
 * VO Rule.
 *
 * @since 18.07.2018
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "rule")
public class Rule {

    @XmlAttribute(name = "filename", required = true)
    private String filename;

    @XmlElement(name = "rule-type", required = true)
    private ApRule.RuleType ruleType;

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public ApRule.RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(final ApRule.RuleType ruleType) {
        this.ruleType = ruleType;
    }
}
