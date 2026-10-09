package cz.tacr.elza.packageimport.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import cz.tacr.elza.domain.RulEntityRule;

/**
 * One rule of an entity rule set:
 * {@code <entity-rule filename="available_items/PERSON/PT_NAME.drl" kind="AVAILABLE_ITEMS"
 * ap-type="PERSON" part-type="PT_NAME" priority="100"/>}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "entity-rule")
public class EntityRule {

    /** DRL file, relative to {@code rul_rule_set/<RS>/rules/}. */
    @XmlAttribute(name = "filename", required = true)
    private String filename;

    @XmlAttribute(name = "kind", required = true)
    private RulEntityRule.Kind kind;

    /** Entity class; the rule applies also to its subclasses. Omitted: all classes. */
    @XmlAttribute(name = "ap-type")
    private String apType;

    /** Part type; omitted: all parts. */
    @XmlAttribute(name = "part-type")
    private String partType;

    @XmlAttribute(name = "priority", required = true)
    private Integer priority;

    /** Version of the package from which the rule is incompatible: entities are revalidated. */
    @XmlAttribute(name = "compatibility-rul-package")
    private Integer compatibilityRulPackage;

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public RulEntityRule.Kind getKind() {
        return kind;
    }

    public void setKind(final RulEntityRule.Kind kind) {
        this.kind = kind;
    }

    public String getApType() {
        return apType;
    }

    public void setApType(final String apType) {
        this.apType = apType;
    }

    public String getPartType() {
        return partType;
    }

    public void setPartType(final String partType) {
        this.partType = partType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public Integer getCompatibilityRulPackage() {
        return compatibilityRulPackage;
    }

    public void setCompatibilityRulPackage(final Integer compatibilityRulPackage) {
        this.compatibilityRulPackage = compatibilityRulPackage;
    }
}
