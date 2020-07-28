package cz.tacr.elza.packageimport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.domain.RulStructuredTypeExtension;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.StructureExtension;
import cz.tacr.elza.packageimport.xml.StructureExtensionDefinition;
import cz.tacr.elza.packageimport.xml.StructureExtensionDefinitions;
import cz.tacr.elza.packageimport.xml.StructureExtensions;
import cz.tacr.elza.repository.ComponentRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructuredTypeExtensionRepository;
import cz.tacr.elza.service.StructObjService;

/**
 * Update extensions for structured types
 *
 *
 */
public class StructTypeExtensionUpdater
{
    final private StructuredTypeExtensionRepository structureExtensionRepository;

    final private StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;

	final private ComponentRepository componentRepository;

	private StructObjService structureService;

	/**
	 * Updated and new structured extensions
	 */
    private List<RulStructuredTypeExtension> structExts;

    /**
     * Original structured extensions
     */
    private List<RulStructuredTypeExtension> origStructExts;

    private List<RulStructuredTypeExtension> rulStructureExtensionsDelete;

    public StructTypeExtensionUpdater(StructuredTypeExtensionRepository structureExtensionRepository,
            StructureExtensionDefinitionRepository structureExtensionDefinitionRepository,
            ComponentRepository componentRepository,
            StructObjService structureService) {
        this.structureExtensionRepository = structureExtensionRepository;
        this.structureExtensionDefinitionRepository = structureExtensionDefinitionRepository;
        this.componentRepository = componentRepository;
        this.structureService = structureService;
    }

    private String getZipDir(final RulStructureExtensionDefinition extensionDefinition) {
        switch (extensionDefinition.getDefType()) {
            case ATTRIBUTE_TYPES:
                return PackageService.ZIP_DIR_RULES;
			case PARSE_VALUE:
            case SERIALIZED_VALUE:
                return PackageService.ZIP_DIR_SCRIPTS;
            default:
                throw new NotImplementedException("Def type: " + extensionDefinition.getDefType());
        }
    }

    /**
     * Process extension definitions
     * @param extDefs
     * @param puc
     * @return
     */
    private void procExtDefs(final StructureExtensionDefinitions extDefs, final PackageContext puc) {
	    // get current definitions
        List<RulStructureExtensionDefinition> rulStructureExtensionDefinitions = this.origStructExts.size() == 0
				? Collections.emptyList()
				: structureExtensionDefinitionRepository.findByRulPackageAndStructuredTypeExtensionIn(puc.getPackage(),
				                                                                                      origStructExts);

		// updated and new extension definitions
		List<RulStructureExtensionDefinition> rulStructureExtensionDefinitionsNew = new ArrayList<>();

		// update extension definitions
		if (extDefs != null && !CollectionUtils.isEmpty(extDefs.getStructureExtensions())) {
			for (StructureExtensionDefinition structureExtensionDefinition : extDefs
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

                convertDef(puc.getPackage(), structureExtensionDefinition, item);
				rulStructureExtensionDefinitionsNew.add(item);
			}
		}

		rulStructureExtensionDefinitionsNew = structureExtensionDefinitionRepository
				.saveAll(rulStructureExtensionDefinitionsNew);

		List<RulStructureExtensionDefinition> rulStructureDefinitionDelete = new ArrayList<>(
				rulStructureExtensionDefinitions);
		rulStructureDefinitionDelete.removeAll(rulStructureExtensionDefinitionsNew);

		List<RulComponent> rulComponentsDelete = rulStructureDefinitionDelete.stream()
				.map(RulStructureExtensionDefinition::getComponent).collect(Collectors.toList());
		structureExtensionDefinitionRepository.deleteAll(rulStructureDefinitionDelete);
		componentRepository.deleteAll(rulComponentsDelete);

		Set<RulStructuredTypeExtension> revalidateStructureExtensions = new HashSet<>();
		try {
			for (RulStructureExtensionDefinition definition : rulStructureDefinitionDelete) {
				if (definition.getDefType() == RulStructureExtensionDefinition.DefType.SERIALIZED_VALUE) {
					revalidateStructureExtensions.add(definition.getStructuredTypeExtension());
				}
			}

			for (RulStructureExtensionDefinition definition : rulStructureExtensionDefinitionsNew) {
				File file = puc.saveFile(puc.getDir(definition), getZipDir(definition),
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
	}

	private void convertDef(final RulPackage rulPackage,
			final StructureExtensionDefinition xmlExtDef, final RulStructureExtensionDefinition dbExtDef) {
	    dbExtDef.setDefType(xmlExtDef.getDefType());
	    dbExtDef.setPriority(xmlExtDef.getPriority());
	    dbExtDef.setRulPackage(rulPackage);

		// find extension
        RulStructuredTypeExtension structExt = this.structExts.stream()
                .filter(x -> x.getCode()
                        .equals(xmlExtDef
                                .getStructureExtension()))
                .findFirst().orElse(null);
        if(structExt==null) {
            throw new BusinessException(
                    "Not found structured type for extension definition",
                    BaseCode.ID_NOT_EXIST)
                            .set("code", xmlExtDef
                                    .getStructureExtension());
        }
        dbExtDef.setStructuredTypeExtension(structExt);

		String filename = xmlExtDef.getFilename();
		if (filename != null) {
			RulComponent component = dbExtDef.getComponent();
			if (component == null) {
				component = new RulComponent();
			}
			component.setFilename(filename);
			componentRepository.save(component);
			dbExtDef.setComponent(component);
		} else {
			RulComponent component = dbExtDef.getComponent();
			dbExtDef.setComponent(null);
			if (component != null) {
				structureExtensionDefinitionRepository.save(dbExtDef);
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

	private void procExtensions(final StructureExtensions structureExtensions,
	                                        final RulPackage rulPackage,
	                                        final List<RulStructuredType> rulStructureTypes)
	{
	    // prepare list of current extensions
	    origStructExts = rulStructureTypes.size() == 0 ? Collections.emptyList() :
                structureExtensionRepository.findByRulPackageAndStructuredTypeIn(rulPackage, rulStructureTypes);

        // List of new extensions
        List<RulStructuredTypeExtension> rulStructureExtensionsNew = new ArrayList<>();
        // iterate extensions from XML
        if (structureExtensions != null && !CollectionUtils.isEmpty(structureExtensions.getStructureExtensions())) {
            for (StructureExtension structureExtension : structureExtensions.getStructureExtensions()) {

                // get existing extension
                RulStructuredTypeExtension item = origStructExts.stream()
                        .filter((r) -> r.getCode().equals(structureExtension.getCode()))
                        .filter((r) -> r.getStructuredType().getCode().equals(structureExtension.getStructureType()))
                        .findFirst()
                        .orElse(null);

                if (item == null) {
                    item = new RulStructuredTypeExtension();
                }

                // update extension
                convertRulStructureExtension(rulPackage, structureExtension, item, rulStructureTypes);
                rulStructureExtensionsNew.add(item);
            }
        }

        this.structExts = structureExtensionRepository.saveAll(rulStructureExtensionsNew);

        // prepare list of extensions for delete
        List<RulStructuredTypeExtension> rulStructureExtensionsDelete = new ArrayList<>(origStructExts);
        rulStructureExtensionsDelete.removeAll(rulStructureExtensionsNew);
        this.rulStructureExtensionsDelete = rulStructureExtensionsDelete;
    }

	public void run(PackageContext ruc) {
        StructureExtensions xmlExtensions = PackageUtils.convertXmlStreamToObject(StructureExtensions.class,
        		ruc.getByteStream(PackageService.STRUCTURE_EXTENSION_XML));
        StructureExtensionDefinitions xmlExtDefs = PackageUtils.convertXmlStreamToObject(StructureExtensionDefinitions.class,
        		ruc.getByteStream(PackageService.STRUCTURE_EXTENSION_DEFINITION_XML));

        procExtensions(xmlExtensions, ruc.getPackage(), ruc.getStructuredTypes());
        procExtDefs(xmlExtDefs, ruc);

        // Remove unused extensions
        if(CollectionUtils.isNotEmpty(rulStructureExtensionsDelete)) {
            structureExtensionRepository.deleteAll(rulStructureExtensionsDelete);
        }
	}
}
