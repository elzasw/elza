package cz.tacr.elza.drools;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.kie.api.io.ResourceType;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.repository.PackageRulesRepository;


/**
 * Abstraktní třída pro Drools pravidla.
 *
 * @author Martin Šlapa
 * @since 26.11.2015
 */
public abstract class Rules {

    @Autowired
    protected PackageRulesRepository packageRulesRepository;

    /**
     * cesta k souboru
     */
    private Path ruleFile;

    /**
     * znalostní báze
     */
    protected KnowledgeBase kbase;

    /**
     * poslední úprava souboru
     */
    private FileTime lastModifiedTime;


    /**
     * Metoda pro kontrolu aktuálnosti souboru s pravidly.
     */
    /*protected void preExecute() throws Exception {
        FileTime ft = Files.getLastModifiedTime(ruleFile);
        if (lastModifiedTime == null || ft.compareTo(lastModifiedTime) > 0) {
            reloadRules();
            lastModifiedTime = ft;
        }
    }*/

    /**
     * Přenačtení souboru s pravidly.
     */
    private void reloadRules() throws Exception {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        kbuilder.add(ResourceFactory.newInputStreamResource(new FileInputStream(ruleFile.toFile()), "UTF-8"),
                ResourceType.DRL);

        if (kbuilder.hasErrors()) {
            throw new RuntimeException("Fail to parse rule: " + kbuilder.getErrors());
        }
        KnowledgeBase tmpKbase = KnowledgeBaseFactory.newKnowledgeBase();
        tmpKbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        kbase = tmpKbase;
    }

    /**
     * Vytvoří novou session.
     *
     * @param rulRuleSet typ pravidel verze
     * @param path
     * @return nová session
     */
    public synchronized StatelessKieSession createNewStatelessKieSession(final RulRuleSet rulRuleSet, final Path path) throws Exception {
    	// Reload rules each time
        ruleFile = path;
        reloadRules();

        return kbase.newStatelessKieSession();
    }

    /**
     * Provede vyvolání scriptu.
     *  @param session session
     * @param objects vstupní data
     * @param ruleFile
     */
    protected final synchronized void execute(final StatelessKieSession session,
                                              final List objects,
                                              final Path ruleFile)
            throws Exception {
        this.ruleFile = ruleFile;
        reloadRules();
        session.execute(objects);
    }

}
