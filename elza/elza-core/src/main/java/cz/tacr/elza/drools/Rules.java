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

import org.kie.api.io.ResourceType;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.ArrangementRuleRepository;
import cz.tacr.elza.repository.PackageDependencyRepository;
import cz.tacr.elza.repository.PackageRepository;


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

    @Autowired
    protected StaticDataService staticDataService;


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
            throw new SystemException("Fail to parse rule: " + kbuilder.getErrors() + 
                    ", file: " + path.toString());
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

    protected void sortDefinitionByPackages(final List<RulStructureExtensionDefinition> rulStructureExtensionDefinitions) {
        rulStructureExtensionDefinitions.sort((o1, o2) -> {

            // 1. seřadit podle priority
            Integer pr1 = o1.getPriority();
            Integer pr2 = o1.getPriority();

            int prComp = pr1.compareTo(pr2);
            if (prComp != 0) {
                return prComp;
            } else {

                // 2. seřadit podle id
                return o1.getStructureExtensionDefinitionId().compareTo(o2.getStructureExtensionDefinitionId());
            }
        });
    }
}
