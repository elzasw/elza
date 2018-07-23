package cz.tacr.elza.packageimport.xml;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * VO FragmentType.
 *
 * @since 01.11.2017
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fragment-type")
public class FragmentType {

    @XmlAttribute(name = "code", required = true)
    private String code;

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlElement(name = "fragment-rule", required = true)
    private List<FragmentRule> rules;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<FragmentRule> getRules() {
        return rules;
    }

    public void setRules(final List<FragmentRule> rules) {
        this.rules = rules;
    }
}
