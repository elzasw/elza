package cz.tacr.elza.core.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureDefinition.DefType;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;

/**
 * Structured type
 *
 */
public class StructType {
    final protected RulStructuredType structuredType;

    /**
     * Definitions
     */
    final protected List<RulStructureDefinition> definitions;

    final protected Map<DefType, List<RulStructureDefinition>> defsByType;

    final protected List<StructTypeExtension> extensions;

    final protected Map<String, StructTypeExtension> extensionsByCode;

    final protected Map<Integer, StructTypeExtension> extensionsById;

    public StructType(final RulStructuredType structuredType,
                      final List<RulStructureDefinition> definitions,
                      final List<RulStructuredTypeExtension> structTypeExts,
                      final List<RulStructureExtensionDefinition> extDefs) {
        this.structuredType = structuredType;
        this.definitions = definitions;
        if (definitions != null) {
            defsByType = definitions.stream()
                    .sorted((a, b) -> a.getPriority().compareTo(b.getPriority()))
                    .collect(Collectors.groupingBy(RulStructureDefinition::getDefType));
        } else {
            defsByType = Collections.emptyMap();
        }
        if (structTypeExts != null) {
            extensions = structTypeExts.stream()
                    .map(ext -> new StructTypeExtension(ext))
                    .collect(Collectors.toList());
            extensionsByCode = extensions.stream().collect(Collectors.toMap(StructTypeExtension::getCode,
                                                                            Function.identity()));
            extensionsById = extensions.stream().collect(Collectors.toMap(StructTypeExtension::getId,
                                                                               Function.identity()));
        } else {
            extensions = Collections.emptyList();
            extensionsByCode = Collections.emptyMap();
            extensionsById = Collections.emptyMap();
        }
        // process definitions
        extDefs.forEach(extDef -> {
            StructTypeExtension ext = extensionsById.get(extDef.getStructuredTypeExtension()
                    .getStructuredTypeExtensionId());
            ext.addDefinition(extDef);
        });
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

    public List<RulStructureDefinition> getDefinitions() {
        return definitions;
    }

    public List<RulStructureDefinition> getDefsByType(DefType defType) {
        return defsByType.getOrDefault(defType, Collections.emptyList());
    }

    public StructTypeExtension getExtByCode(String extCode) {
        return extensionsByCode.get(extCode);
    }

}
