package cz.tacr.elza.drools;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;


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
                .findByStructTypeAndDefTypeOrderByPriority(fragmentType, RulStructureDefinition.DefType.ATTRIBUTE_TYPES);

        for (RulStructureDefinition rulStructureDefinition : rulStructureDefinitions) {
            // TODO: Consider using structureType in getDroolsFile?
            Path path = resourcePathResolver.getDroolsFile(rulStructureDefinition);

            KieSession session = createKieSession(path);
            executeSession(session, facts);
        }

        List<RulStructureExtensionDefinition> rulStructureExtensionDefinitions = structureExtensionDefinitionRepository
                .findByStructureTypeAndDefTypeOrderByPriority(fragmentType, RulStructureExtensionDefinition.DefType.ATTRIBUTE_TYPES);

        sortDefinitionByPackages(rulStructureExtensionDefinitions);

        for (RulStructureExtensionDefinition rulStructureExtensionDefinition : rulStructureExtensionDefinitions) {
            // TODO: Consider using structureType in getDroolsFile?
            Path path = resourcePathResolver.getDroolsFile(rulStructureExtensionDefinition);

            KieSession session = createKieSession(path);
            executeSession(session, facts);
        }

        return rulDescItemTypeExtList;
    }

}
