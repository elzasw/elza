package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition.DefType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;

public class StructTypeExtension {

    private final RulStructuredTypeExtension entity;
    private List<RulStructureExtensionDefinition> definitions = new ArrayList<>(0);
    private Map<DefType, List<RulStructureExtensionDefinition>> defsByType = new HashMap<>();

    public StructTypeExtension(final RulStructuredTypeExtension ext) {
        this.entity = ext;
    }

    public RulStructuredTypeExtension getEntity() {
        return entity;
    }

    public String getCode() {
        return entity.getCode();
    }

    public Integer getId() {
        return entity.getStructuredTypeExtensionId();
    }

    void addDefinition(RulStructureExtensionDefinition extDef) {
        definitions.add(extDef);
        List<RulStructureExtensionDefinition> defs = defsByType.computeIfAbsent(extDef.getDefType(),
                                                                                defType -> new ArrayList<>(1));
        defs.add(extDef);
    }

    public List<RulStructureExtensionDefinition> getDefsByType(DefType defType) {
        return defsByType.get(defType);
    }
}
