package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * VO RuleSystem.
 *
 * @since 01.11.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "rule-system")
public class RuleSystem {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlElement(name = "rule", required = true)
    private List<Rule> rules;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(final List<Rule> rules) {
        this.rules = rules;
    }
}
