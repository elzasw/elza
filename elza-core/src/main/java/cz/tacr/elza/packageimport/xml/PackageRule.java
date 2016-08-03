package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.api.RulRule;


/**
 * VO PackageRule.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "package-rule")
public class PackageRule {

    @XmlAttribute(name = "rule-set", required = true)
    private String ruleSet;

    @XmlAttribute(name = "filename", required = true)
    private String filename;

    @XmlAttribute(name = "output-type")
    private String outputType;

    @XmlElement(name = "rule-type", required = true)
    private RulRule.RuleType ruleType;

    @XmlElement(name = "priority", required = true)
    private Integer priority;

    public String getRuleSet() {
        return ruleSet;
    }

    public void setRuleSet(final String ruleSet) {
        this.ruleSet = ruleSet;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(final String outputType) {
        this.outputType = outputType;
    }

    public RulRule.RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(final RulRule.RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }
}
