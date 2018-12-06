package cz.tacr.elza.drools;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulPackageDependency;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.packageimport.PackageUtils;
import cz.tacr.elza.repository.PackageDependencyRepository;
import cz.tacr.elza.repository.PackageRepository;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.ArrangementRuleRepository;


/**
 * Abstraktní třída pro Drools pravidla.
 *
 * @author Martin Šlapa
 * @since 26.11.2015
 */
public abstract class Rules {

    /**
     * uchování informace o načtených drools souborech
     */
    private Map<Path, Map.Entry<FileTime, KnowledgeBase>> rulesByPathMap = new HashMap<>();

    @Autowired
    protected ArrangementRuleRepository arrangementRuleRepository;

    @Autowired
    protected PackageRepository packageRepository;

    @Autowired
    protected PackageDependencyRepository packageDependencyRepository;


    /**
     * Metoda pro kontrolu aktuálnosti souboru s pravidly.
     * 
     * @throws IOException
     */
    protected Map.Entry<FileTime, KnowledgeBase> testChangeFile(final Path path,
                                                                final Map.Entry<FileTime, KnowledgeBase> entry)
            throws IOException {
        FileTime ft = Files.getLastModifiedTime(path);
        if (entry.getKey() == null || ft.compareTo(entry.getKey()) > 0) {
            Map.Entry<FileTime, KnowledgeBase> entryNew = reloadRules(path);
            rulesByPathMap.remove(path);
            rulesByPathMap.put(path, entryNew);
            return entryNew;
        } else {
            return entry;
        }
    }

    /**
     * Přenačtení souboru s pravidly.
     * 
     * @throws IOException
     */
    private Map.Entry<FileTime, KnowledgeBase> reloadRules(final Path path) throws IOException {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add(ResourceFactory.newInputStreamResource(new FileInputStream(path.toFile()), "UTF-8"),
                ResourceType.DRL);

        if (kbuilder.hasErrors()) {
            throw new SystemException("Fail to parse rule: " + kbuilder.getErrors());
        }
        KnowledgeBase tmpKbase = KnowledgeBaseFactory.newKnowledgeBase();
        tmpKbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        FileTime ft = Files.getLastModifiedTime(path);
        return new AbstractMap.SimpleEntry<>(ft, tmpKbase);
    }

    /**
     * Vytvoří novou session.
     *
     * @param path
     * @return nová session
     * @throws IOException
     */
    public synchronized StatelessKieSession createNewStatelessKieSession(final Path path) throws IOException {

        Map.Entry<FileTime, KnowledgeBase> entry = rulesByPathMap.get(path);

        if (entry == null) {
            entry = reloadRules(path);
            rulesByPathMap.put(path, entry);
        } else {
            entry = testChangeFile(path, entry);
        }

        return entry.getValue().newStatelessKieSession();
    }

    protected List<RulPackage> getSortedPackages(final List<RulPackage> packages) {
        List<RulPackage> packagesAll = packageRepository.findAll();
        PackageUtils.Graph<RulPackage> g = new PackageUtils.Graph<>(packagesAll.size());
        List<RulPackageDependency> dependencies = packageDependencyRepository.findAll();
        dependencies.forEach(d -> g.addEdge(d.getRulPackage(), d.getDependsOnPackage()));
        List<RulPackage> rulPackages = g.topologicalSort();
        rulPackages.retainAll(packages);
        return rulPackages;
    }

    protected void sortDefinitionByPackages(final List<RulStructureExtensionDefinition> rulStructureExtensionDefinitions, final List<RulPackage> sortedPackages) {
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
    }
}
