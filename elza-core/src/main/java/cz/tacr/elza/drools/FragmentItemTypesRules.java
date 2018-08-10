package cz.tacr.elza.drools;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import cz.tacr.elza.repository.StructuredTypeRepository;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Zpracování pravidel typů parametrů pro fragment.
 *
 * @since 03.11.2017
 */
@Component
public class FragmentItemTypesRules extends Rules {

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private StructureDefinitionRepository structureDefinitionRepository;

    @Autowired
    private StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;

    /**
     * Spuštění zpracování pravidel.
     *
     * @param rulDescItemTypeExtList seznam všech atributů
     * @return seznam typů atributů odpovídající pravidlům
     */
    public synchronized List<RulItemTypeExt> execute(final RulStructuredType fragmentType,
                                                     final List<RulItemTypeExt> rulDescItemTypeExtList,
                                                     final List<ApItem> items) throws Exception {
        LinkedList<Object> facts = new LinkedList<>();
        facts.addAll(ModelFactory.createApItems(items));
        facts.addAll(rulDescItemTypeExtList);

        List<RulStructureDefinition> rulStructureDefinitions = structureDefinitionRepository
                .findByStructuredTypeAndDefTypeOrderByPriority(fragmentType, RulStructureDefinition.DefType.ATTRIBUTE_TYPES);

        for (RulStructureDefinition rulStructureDefinition : rulStructureDefinitions) {
            // TODO: Consider using structureType in getDroolsFile?
            Path path = resourcePathResolver.getDroolsFile(rulStructureDefinition);

            StatelessKieSession session = createNewStatelessKieSession(path);
            session.execute(facts);
        }

        List<RulStructureExtensionDefinition> rulStructureExtensionDefinitions = structureExtensionDefinitionRepository
                .findByStructureTypeAndDefTypeOrderByPriority(fragmentType, RulStructureExtensionDefinition.DefType.ATTRIBUTE_TYPES);

        List<RulPackage> rulPackages = rulStructureExtensionDefinitions.stream()
                .map(RulStructureExtensionDefinition::getRulPackage).collect(Collectors.toList());

        List<RulPackage> sortedPackages = getSortedPackages(rulPackages);

        sortDefinitionByPackages(rulStructureExtensionDefinitions, sortedPackages);

        for (RulStructureExtensionDefinition rulStructureExtensionDefinition : rulStructureExtensionDefinitions) {
            // TODO: Consider using structureType in getDroolsFile?
            Path path = resourcePathResolver.getDroolsFile(rulStructureExtensionDefinition);

            StatelessKieSession session = createNewStatelessKieSession(path);
            session.execute(facts);
        }

        return rulDescItemTypeExtList;
    }

}
