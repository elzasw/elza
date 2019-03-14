package cz.tacr.elza.core.data;

import java.util.List;

import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructuredType;

/**
 * Structured type
 *
 */
public class StructType {
    final protected RulStructuredType structuredType;

    /**
     * Attribute definitions
     */
    final protected List<RulStructureDefinition> attrDefs;

    public StructType(final RulStructuredType structuredType,
                      final List<RulStructureDefinition> attrDefs) {
        this.structuredType = structuredType;
        this.attrDefs = attrDefs;
    }

    public RulStructuredType getStructuredType() {
        return structuredType;
    }

    public Integer getStructuredTypeId() {
        return structuredType.getStructuredTypeId();
    }

    public String getCode() {
        return structuredType.getCode();
    }

    public List<RulStructureDefinition> getAttrDefs() {
        return attrDefs;
    }

}
