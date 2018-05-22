package cz.tacr.elza.packageimport;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.packageimport.xml.StructureExtension;
import cz.tacr.elza.packageimport.xml.StructureExtensionDefinition;
import cz.tacr.elza.packageimport.xml.StructureExtensionDefinitions;
import cz.tacr.elza.packageimport.xml.StructureExtensions;
import cz.tacr.elza.repository.ComponentRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructuredTypeExtensionRepository;
import cz.tacr.elza.service.StructureService;

/**
 * Update for structured type extensions
 * 
 *
 */
public class StructTypeExtensionUpdater	
{
    final private StructuredTypeExtensionRepository structureExtensionRepository;
    
    final private StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;

	final private ComponentRepository componentRepository;
	
	private StructureService structureService;
	

    private String getZipDir(final RulStructureExtensionDefinition extensionDefinition) {
        switch (extensionDefinition.getDefType()) {
            case ATTRIBUTE_TYPES:
                return PackageService.ZIP_DIR_RULES;
            case SERIALIZED_VALUE:
                return PackageService.ZIP_DIR_SCRIPTS;
            default:
                throw new NotImplementedException("Def type: " + extensionDefinition.getDefType());
        }
    }	
    
    public StructTypeExtensionUpdater(StructuredTypeExtensionRepository structureExtensionRepository,
    		StructureExtensionDefinitionRepository structureExtensionDefinitionRepository, ComponentRepository componentRepository,
    		StructureService structureService
    		) {
    	this.structureExtensionRepository = structureExtensionRepository;
    	this.structureExtensionDefinitionRepository = structureExtensionDefinitionRepository;
    	this.componentRepository = componentRepository;
    	this.structureService = structureService;
    }
    
	private List<RulStructureExtensionDefinition> processStructureExtensionDefinitions(
			final StructureExtensionDefinitions structureExtensionDefinitions, final RuleUpdateContext ruc,
			final List<RulStructuredTypeExtension> rulStructureExtensionList) {
		List<RulStructureExtensionDefinition> rulStructureExtensionDefinitions = rulStructureExtensionList.size() == 0
				? Collections.emptyList()
				: structureExtensionDefinitionRepository.findByRulPackageAndStructuredTypeExtensionIn(ruc.getRulPackage(),
						rulStructureExtensionList);

		List<RulStructureExtensionDefinition> rulStructureExtensionDefinitionsNew = new ArrayList<>();

		if (structureExtensionDefinitions != null
				&& !CollectionUtils.isEmpty(structureExtensionDefinitions.getStructureExtensions())) {
			for (StructureExtensionDefinition structureExtensionDefinition : structureExtensionDefinitions
					.getStructureExtensions()) {

				RulStructureExtensionDefinition item = rulStructureExtensionDefinitions.stream()
						.filter((r) -> r.getComponent().getFilename()
								.equals(structureExtensionDefinition.getFilename()))
						.filter((r) -> r.getStructuredTypeExtension().getCode()
								.equals(structureExtensionDefinition.getStructureExtension()))
						.findFirst().orElse(null);

				if (item == null) {
					item = new RulStructureExtensionDefinition();
				}

				convertRulStructureExtensionDefinition(ruc.getRulPackage(), structureExtensionDefinition, item,
						rulStructureExtensionList);
				rulStructureExtensionDefinitionsNew.add(item);
			}
		}

		rulStructureExtensionDefinitionsNew = structureExtensionDefinitionRepository
				.save(rulStructureExtensionDefinitionsNew);

		List<RulStructureExtensionDefinition> rulStructureDefinitionDelete = new ArrayList<>(
				rulStructureExtensionDefinitions);
		rulStructureDefinitionDelete.removeAll(rulStructureExtensionDefinitionsNew);

		List<RulComponent> rulComponentsDelete = rulStructureDefinitionDelete.stream()
				.map(RulStructureExtensionDefinition::getComponent).collect(Collectors.toList());
		structureExtensionDefinitionRepository.delete(rulStructureDefinitionDelete);
		componentRepository.delete(rulComponentsDelete);

		Set<RulStructuredTypeExtension> revalidateStructureExtensions = new HashSet<>();
		try {
			for (RulStructureExtensionDefinition definition : rulStructureDefinitionDelete) {
				if (definition.getDefType() == RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE) {
					revalidateStructureExtensions.add(definition.getStructuredTypeExtension());
				}
			}

			for (RulStructureExtensionDefinition definition : rulStructureExtensionDefinitionsNew) {
				File file = ruc.saveFile(ruc.getDir(definition),
						PackageService.ZIP_DIR_RULE_SET + "/" + ruc.getRulSetCode() + "/" + getZipDir(definition),
						definition.getComponent().getFilename());
				if (definition.getDefType() == RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE) {
					String newHash = PackageUtils.sha256File(file);
					String oldHash = definition.getComponent().getHash();
					if (!StringUtils.equalsIgnoreCase(newHash, oldHash)) {
						definition.getComponent().setHash(newHash);
						componentRepository.save(definition.getComponent());
						revalidateStructureExtensions.add(definition.getStructuredTypeExtension());
					}
				}
			}
		} catch (IOException e) {
			throw new SystemException(e);
		}

		structureService.revalidateStructureExtensions(revalidateStructureExtensions);
		return rulStructureExtensionDefinitionsNew;
	}

	private void convertRulStructureExtensionDefinition(final RulPackage rulPackage,
			final StructureExtensionDefinition structureExtensionDefinition, final RulStructureExtensionDefinition item,
			final List<RulStructuredTypeExtension> rulStructureExtensionList) {
		item.setDefType(structureExtensionDefinition.getDefType());
		item.setPriority(structureExtensionDefinition.getPriority());
		item.setRulPackage(rulPackage);
		item.setStructuredTypeExtension(rulStructureExtensionList.stream()
				.filter(x -> x.getCode().equals(structureExtensionDefinition.getStructureExtension())).findFirst()
				.orElse(null));

		String filename = structureExtensionDefinition.getFilename();
		if (filename != null) {
			RulComponent component = item.getComponent();
			if (component == null) {
				component = new RulComponent();
			}
			component.setFilename(filename);
			componentRepository.save(component);
			item.setComponent(component);
		} else {
			RulComponent component = item.getComponent();
			item.setComponent(null);
			if (component != null) {
				structureExtensionDefinitionRepository.save(item);
				componentRepository.delete(component);
			}
		}
	}    

	private void convertRulStructureExtension(final RulPackage rulPackage, final StructureExtension structureExtension,
			final RulStructuredTypeExtension item, final List<RulStructuredType> rulStructureTypes) {
		item.setCode(structureExtension.getCode());
		item.setName(structureExtension.getName());
		item.setStructuredType(rulStructureTypes.stream()
				.filter(x -> x.getCode().equals(structureExtension.getStructureType())).findFirst().orElse(null));
		item.setRulPackage(rulPackage);
	}
    
	private List<RulStructuredTypeExtension> processStructureExtensions(final StructureExtensions structureExtensions,
                                                                        final RulPackage rulPackage,
                                                                        final List<RulStructuredType> rulStructureTypes) {
        List<RulStructuredTypeExtension> rulStructureExtensions = rulStructureTypes.size() == 0 ? Collections.emptyList() :
                structureExtensionRepository.findByRulPackageAndStructuredTypeIn(rulPackage, rulStructureTypes);
        List<RulStructuredTypeExtension> rulStructureExtensionsNew = new ArrayList<>();

        if (structureExtensions != null && !CollectionUtils.isEmpty(structureExtensions.getStructureExtensions())) {
            for (StructureExtension structureExtension : structureExtensions.getStructureExtensions()) {

                RulStructuredTypeExtension item = rulStructureExtensions.stream()
                        .filter((r) -> r.getCode().equals(structureExtension.getCode()))
                        .filter((r) -> r.getStructuredType().getCode().equals(structureExtension.getStructureType()))
                        .findFirst()
                        .orElse(null);

                if (item == null) {
                    item = new RulStructuredTypeExtension();
                }

                convertRulStructureExtension(rulPackage, structureExtension, item, rulStructureTypes);
                rulStructureExtensionsNew.add(item);
            }
        }

        rulStructureExtensionsNew = structureExtensionRepository.save(rulStructureExtensionsNew);

        List<RulStructuredTypeExtension> rulStructureExtensionsDelete = new ArrayList<>(rulStructureExtensions);
        rulStructureExtensionsDelete.removeAll(rulStructureExtensionsNew);

        structureExtensionRepository.delete(rulStructureExtensionsDelete);

        return rulStructureExtensionsNew;
    }

	public void run(RuleUpdateContext ruc) {
        StructureExtensions structureExtensions = PackageUtils.convertXmlStreamToObject(StructureExtensions.class,
        		ruc.getByteStream(PackageService.STRUCTURE_EXTENSION_XML));
        StructureExtensionDefinitions structureExtensionDefinitions = PackageUtils.convertXmlStreamToObject(StructureExtensionDefinitions.class, 
        		ruc.getByteStream(PackageService.STRUCTURE_EXTENSION_DEFINITION_XML));		
		
        List<RulStructuredTypeExtension> rulStructureExtensionList = processStructureExtensions(structureExtensions, ruc.getRulPackage(), ruc.getStructureTypes());
        List<RulStructureExtensionDefinition> rulStructureExtensionDefinitionList = processStructureExtensionDefinitions(
        		structureExtensionDefinitions, ruc, rulStructureExtensionList);		
	}
}
