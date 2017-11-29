package cz.tacr.elza.drools;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.RulItemTypeExt;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructureType;
import cz.tacr.elza.packageimport.PackageUtils;
import cz.tacr.elza.repository.PackageDependencyRepository;
import cz.tacr.elza.repository.PackageRepository;
import cz.tacr.elza.repository.StructureDefinitionRepository;
import cz.tacr.elza.repository.StructureExtensionDefinitionRepository;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Zpracování pravidel typů parametrů pro strukturovaný typ.
 *
 * @since 03.11.2017
 */
@Component
public class StructureItemTypesRules extends Rules {

    @Autowired
    private RulesConfigExecutor rulesConfigExecutor;

    @Autowired
    private StructureDefinitionRepository structureDefinitionRepository;

    @Autowired
    private StructureExtensionDefinitionRepository structureExtensionDefinitionRepository;

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private PackageDependencyRepository packageDependencyRepository;

    /**
     * Spuštění zpracování pravidel.
     *
     * @param structureType
     * @param rulDescItemTypeExtList seznam všech atributů
     */
    public synchronized List<RulItemTypeExt> execute(final RulStructureType structureType,
                                                     final List<RulItemTypeExt> rulDescItemTypeExtList,
                                                     final ArrFund fund) throws Exception {

        LinkedList<Object> facts = new LinkedList<>();
        facts.addAll(rulDescItemTypeExtList);

        Path path;
        List<RulStructureDefinition> rulStructureDefinitions = structureDefinitionRepository
                .findByStructureTypeAndDefTypeOrderByPriority(structureType, RulStructureDefinition.DefType.ATTRIBUTE_TYPES);

        for (RulStructureDefinition rulStructureDefinition : rulStructureDefinitions) {
            path = Paths.get(rulesConfigExecutor.getDroolsDir(rulStructureDefinition.getRulPackage().getCode(), structureType.getRuleSet().getCode()) + File.separator + rulStructureDefinition.getComponent().getFilename());
            StatelessKieSession session = createNewStatelessKieSession(path);
            execute(session, facts);
        }

        List<RulStructureExtensionDefinition> rulStructureExtensionDefinitions = structureExtensionDefinitionRepository
                .findByStructureTypeAndDefTypeAndFundOrderByPriority(structureType, RulStructureExtensionDefinition.DefType.ATTRIBUTE_TYPES, fund);

        List<RulPackage> rulPackages = rulStructureExtensionDefinitions.stream()
                .map(RulStructureExtensionDefinition::getRulPackage).collect(Collectors.toList());

        List<RulPackage> sortedPackages = getSortedPackages(rulPackages);

        rulStructureExtensionDefinitions.sort((o1, o2) -> {

            RulPackage p1 = o1.getRulPackage();
            RulPackage p2 = o2.getRulPackage();

            // 1. seřadit podle řazení balíčků
            Integer ae1 = sortedPackages.indexOf(p1);
            Integer ae2 = sortedPackages.indexOf(p2);

            int pComp = ae1.compareTo(ae2);
            if (pComp != 0) {
                return pComp;
            } else {

                // 2. seřadit podle priority
                Integer pr1 = o1.getPriority();
                Integer pr2 = o1.getPriority();

                int prComp = pr1.compareTo(pr2);
                if (prComp != 0) {
                    return prComp;
                } else {

                    // 2. seřadit podle id
                    return o1.getStructureExtensionDefinitionId().compareTo(o2.getStructureExtensionDefinitionId());
                }
            }
        });

        for (RulStructureExtensionDefinition rulStructureExtensionDefinition : rulStructureExtensionDefinitions) {
            path = Paths.get(rulesConfigExecutor.getDroolsDir(rulStructureExtensionDefinition.getRulPackage().getCode(), structureType.getRuleSet().getCode()) + File.separator + rulStructureExtensionDefinition.getComponent().getFilename());
            StatelessKieSession session = createNewStatelessKieSession(path);
            execute(session, facts);
        }

        return rulDescItemTypeExtList;
    }

    private List<RulPackage> getSortedPackages(final List<RulPackage> packages) {
        List<RulPackage> packagesAll = packageRepository.findAll();
        PackageUtils.Graph<RulPackage> g = new PackageUtils.Graph<>(packagesAll.size());
        List<RulPackageDependency> dependencies = packageDependencyRepository.findAll();
        dependencies.forEach(d -> g.addEdge(d.getSourcePackage(), d.getTargetPackage()));
        List<RulPackage> rulPackages = g.topologicalSort();
        rulPackages.retainAll(packages);
        return rulPackages;
    }

}
