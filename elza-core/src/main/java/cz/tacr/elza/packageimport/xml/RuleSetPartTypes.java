package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Part types offered by an entity rule set, in display order: {@code rul_rule_set/<RS>/rul_part_type.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "part-types")
@XmlType(name = "rule-set-part-types")
public class RuleSetPartTypes {

    @XmlElement(name = "part-type", required = true)
    private List<Member> partTypes;

    public List<Member> getPartTypes() {
        return partTypes;
    }

    public void setPartTypes(final List<Member> partTypes) {
        this.partTypes = partTypes;
    }

    /**
     * {@code <part-type code="PT_NAME"/>}
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "rule-set-part-type")
    public static class Member {

        @XmlAttribute(name = "code", required = true)
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }
    }
}
