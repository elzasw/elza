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
import java.util.UUID;

import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @since 26.11.2015
 */
public abstract class Rules {

    static private final Logger logger = LoggerFactory.getLogger(Rules.class);

    /**
     * uchování informace o načtených drools souborech
     */
    private Map<Path, Map.Entry<FileTime, KieBase>> rulesByPathMap = new HashMap<>();

    @Autowired
    protected ArrangementRuleRepository arrangementRuleRepository;

    @Autowired
    protected PackageRepository packageRepository;

    @Autowired
    protected PackageDependencyRepository packageDependencyRepository;

    @Autowired
    protected StaticDataService staticDataService;

    // KIE configuration
    private KieBaseConfiguration kieBaseConf;

    public Rules() {
        KieServices ks = KieServices.Factory.get();
        kieBaseConf = ks.newKieBaseConfiguration();
        //kieBaseConf.setOption(SequentialOption.YES);
    }


    /**
     * Metoda pro kontrolu aktuálnosti souboru s pravidly.
     * 
     * @throws IOException
     */
    protected Map.Entry<FileTime, KieBase> testChangeFile(final Path path,
                                                          final Map.Entry<FileTime, KieBase> entry)
            throws IOException {
        FileTime ft = Files.getLastModifiedTime(path);
        if (entry.getKey() == null || ft.compareTo(entry.getKey()) > 0) {
            Map.Entry<FileTime, KieBase> entryNew = reloadRules(path);
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
    private Map.Entry<FileTime, KieBase> reloadRules(final Path path) throws IOException {
        logger.debug("Loading rules: {}", path);

        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        kfs.write(ResourceFactory.newInputStreamResource(new FileInputStream(path.toFile()), "UTF-8").setResourceType(ResourceType.DRL).setTargetPath(UUID.randomUUID().toString()));
        KieBuilder kBuilder = ks.newKieBuilder(kfs);
        kBuilder.buildAll();
        if (kBuilder.getResults().hasMessages(Message.Level.ERROR)) {
            throw new SystemException("Drl pravidlo není validní, file: " + path.toString())
                    .set("detail", kBuilder.getResults().getMessages());
        }
        KieContainer kc = ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
        // set kieBase configuration
        KieBase kbc = kc.newKieBase(kieBaseConf);
        FileTime ft = Files.getLastModifiedTime(path);
        return new AbstractMap.SimpleEntry<>(ft, kbc);
    }

    private KieBase getKieBase(Path path) throws IOException {
        Map.Entry<FileTime, KieBase> entry = rulesByPathMap.get(path);

        if (entry == null) {
            entry = reloadRules(path);
            rulesByPathMap.put(path, entry);
        } else {
            entry = testChangeFile(path, entry);
        }

        KieBase kb = entry.getValue();
        return kb;
    }

    /**
     * Vytvoří novou session.
     *
     * @param path
     * @return nová session
     * @throws IOException
     */
    public synchronized KieSession createKieSession(final Path path) throws IOException {

        logger.debug("Creating KieSession for rules: {}", path);

        KieBase kb = getKieBase(path);

        KieSession ksession = kb.newKieSession();
        return ksession;
    }

    public synchronized StatelessKieSession createKieStatelessSession(final Path path) throws IOException {

        logger.debug("Creating StatelessKieSession for rules: {}", path);

        KieBase kb = getKieBase(path);

        StatelessKieSession ksession = kb.newStatelessKieSession();
        return ksession;
    }

    /**
     * Execute with stateless session
     */
    public void executeStateless(StatelessKieSession ksession, List<Object> facts) {
        ksession.execute(facts);
    }

    public void executeSession(KieSession ksession, List<Object> facts) {

        for (Object fact : facts) {
            ksession.insert(fact);
        }
        ksession.fireAllRules();
        ksession.dispose();
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
