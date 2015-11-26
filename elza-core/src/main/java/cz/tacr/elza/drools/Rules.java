package cz.tacr.elza.drools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.kie.api.io.ResourceType;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;


/**
 * Abstraktní třída pro Drools pravidla.
 *
 * @author Martin Šlapa
 * @since 26.11.2015
 */
public abstract class Rules {

    /**
     * cesta k souboru
     */
    private final Path ruleFile;

    /**
     * znalostní báze
     */
    protected KnowledgeBase kbase;

    /**
     * poslední úprava souboru
     */
    private FileTime lastModifiedTime;

    /**
     * Kontruktor třídy.
     *
     * @param ruleFile cesta k souboru
     */
    public Rules(Path ruleFile) {
        this.ruleFile = ruleFile;
    }

    /**
     * Metoda pro kontrolu aktuálnosti souboru s pravidly.
     */
    protected void preExecute() throws Exception {
        FileTime ft = Files.getLastModifiedTime(ruleFile);
        if (lastModifiedTime == null || ft.compareTo(lastModifiedTime) > 0) {
            reloadRules();
            lastModifiedTime = ft;
        }
    }

    /**
     * Přenačtení souboru s pravidly.
     */
    private void reloadRules() throws Exception {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newFileResource(ruleFile.toFile()), ResourceType.DRL);
        if (kbuilder.hasErrors()) {
            throw new RuntimeException("Fail to parse rule: " + kbuilder.getErrors());
        }
        KnowledgeBase tmpKbase = KnowledgeBaseFactory.newKnowledgeBase();
        tmpKbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        kbase = tmpKbase;
    }

    /**
     * @return vrací cestu k souboru
     */
    public Path getRuleFile() {
        return ruleFile;
    }
}
