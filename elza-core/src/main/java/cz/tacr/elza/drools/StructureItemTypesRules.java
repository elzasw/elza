package cz.tacr.elza.drools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.drools.service.ModelFactory;
import cz.tacr.elza.packageimport.PackageUtils;
import cz.tacr.elza.repository.PackageDependencyRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;


/**
 * Zpracování pravidel typů parametrů pro strukturovaný typ.
 *
 * @since 03.11.2017
 */
@Component
public class StructureItemTypesRules extends Rules {

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private StructureDefinitionRepository structureDefinitionRepository;

    @Autowired
    private StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;

    /**
     * Spuštění zpracování pravidel.
     *
     * @param structureType
     *            typ
     * @param rulDescItemTypeExtList
     *            seznam všech atributů
     * @param structureItems
     *            seznam položek strukturovaného datového typu
     * @return seznam typů atributů odpovídající pravidlům
     * @throws IOException
     */
    public synchronized List<RulItemTypeExt> execute(final RulStructuredType structureType,
                                                     final List<RulItemTypeExt> rulDescItemTypeExtList,
                                                     final ArrFund fund,
                                                     final List<ArrStructuredItem> structureItems)
            throws IOException {

        LinkedList<Object> facts = new LinkedList<>();
        facts.addAll(ModelFactory.createStructuredItems(structureItems));
        facts.addAll(rulDescItemTypeExtList);

        List<RulStructureDefinition> rulStructureDefinitions = structureDefinitionRepository
                .findByStructuredTypeAndDefTypeOrderByPriority(structureType, RulStructureDefinition.DefType.ATTRIBUTE_TYPES);

        for (RulStructureDefinition rulStructureDefinition : rulStructureDefinitions) {
            // TODO: Consider using structureType in getDroolsFile?
            Path path = resourcePathResolver.getDroolsFile(rulStructureDefinition);

            StatelessKieSession session = createNewStatelessKieSession(path);
            session.execute(facts);
        }

        List<RulStructureExtensionDefinition> rulStructureExtensionDefinitions = structureExtensionDefinitionRepository
                .findByStructureTypeAndDefTypeAndFundOrderByPriority(structureType, RulStructureExtensionDefinition.DefType.ATTRIBUTE_TYPES, fund);

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
