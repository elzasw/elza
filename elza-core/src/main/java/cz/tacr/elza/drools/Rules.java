package cz.tacr.elza.drools;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.exception.SystemException;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;

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


    /**
     * Metoda pro kontrolu aktuálnosti souboru s pravidly.
     */
    protected Map.Entry<FileTime, KnowledgeBase> testChangeFile(final Path path, final Map.Entry<FileTime, KnowledgeBase> entry) throws Exception {
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
     */
    private Map.Entry<FileTime, KnowledgeBase> reloadRules(final Path path) throws Exception {
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
     */
    public synchronized StatelessKieSession createNewStatelessKieSession(final Path path) throws Exception {

        Map.Entry<FileTime, KnowledgeBase> entry = rulesByPathMap.get(path);

        if (entry == null) {
            entry = reloadRules(path);
            rulesByPathMap.put(path, entry);
        } else {
            entry = testChangeFile(path, entry);
        }

        return entry.getValue().newStatelessKieSession();
    }

    /**
     * Provede vyvolání scriptu.
     *  @param session session
     * @param objects vstupní data
     */
    protected final synchronized void execute(final StatelessKieSession session,
                                              final List objects)
            throws Exception {
        session.execute(objects);
    }

}
