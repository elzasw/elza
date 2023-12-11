package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import cz.tacr.elza.domain.RulArrangementRule;


/**
 * VO ArrangementRule.
 *
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "arrangement-rule")
public class ArrangementRule {

    @XmlAttribute(name = "filename", required = true)
    private String filename;

    @XmlElement(name = "rule-type", required = true)
    private RulArrangementRule.RuleType ruleType;

    @XmlElement(name = "priority", required = true)
    private Integer priority;

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public RulArrangementRule.RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(final RulArrangementRule.RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }
}
