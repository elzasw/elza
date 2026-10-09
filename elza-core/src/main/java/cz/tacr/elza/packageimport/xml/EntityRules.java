package cz.tacr.elza.packageimport.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Rules of an entity rule set: {@code rul_rule_set/<RS>/rul_entity_rule.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "entity-rules")
@XmlType(name = "entity-rules")
public class EntityRules {

    @XmlElement(name = "entity-rule", required = true)
    private List<EntityRule> entityRules;

    public List<EntityRule> getEntityRules() {
        return entityRules;
    }

    public void setEntityRules(final List<EntityRule> entityRules) {
        this.entityRules = entityRules;
    }
}
