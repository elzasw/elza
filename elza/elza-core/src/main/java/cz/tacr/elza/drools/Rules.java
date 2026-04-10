package cz.tacr.elza.drools;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.drools.core.event.DefaultAgendaEventListener;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.event.rule.AfterMatchFiredEvent;
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

    private static final Logger logger = LoggerFactory.getLogger(Rules.class);

    private record CachedKieBase(FileTime lastModified, KieBase kieBase) {}

    /**
     * uchování informace o načtených drools souborech
     */
    private static final Map<Path, CachedKieBase> rulesByPathMap = new ConcurrentHashMap<>();

    private static final KieBaseConfiguration kieBaseConf;

    static {
        KieServices ks = KieServices.Factory.get();
        kieBaseConf = ks.newKieBaseConfiguration();
    }

    @Autowired
    protected ArrangementRuleRepository arrangementRuleRepository;

    @Autowired
    protected PackageRepository packageRepository;

    @Autowired
    protected PackageDependencyRepository packageDependencyRepository;

    @Autowired
    protected StaticDataService staticDataService;

    /**
     * Přenačtení souboru s pravidly.
     *
     * @throws IOException
     */
    private static synchronized CachedKieBase reloadRules(final Path path) throws IOException {
        CachedKieBase existing = rulesByPathMap.get(path);
        if (existing != null) {
            FileTime ft = Files.getLastModifiedTime(path);
            if (existing.lastModified() != null && ft.compareTo(existing.lastModified()) <= 0) {
                return existing;
            }
        }

        logger.debug("Loading rules: {}", path);

        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            kfs.write(ResourceFactory.newInputStreamResource(fis, "UTF-8")
                    .setResourceType(ResourceType.DRL)
                    .setTargetPath(UUID.randomUUID().toString()));
            KieBuilder kBuilder = ks.newKieBuilder(kfs);
            kBuilder.buildAll();
            if (kBuilder.getResults().hasMessages(Message.Level.ERROR)) {
                throw new SystemException("Drl pravidlo není validní, file: " + path)
                        .set("detail", kBuilder.getResults().getMessages());
            }
        }

        KieContainer kc = ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
        KieBase kbc = kc.newKieBase(kieBaseConf);
        FileTime ft = Files.getLastModifiedTime(path);
        CachedKieBase cached = new CachedKieBase(ft, kbc);
        rulesByPathMap.put(path, cached);
        return cached;
    }

    private KieBase getKieBase(Path path) throws IOException {
        CachedKieBase cached = rulesByPathMap.get(path);
        if (cached != null) {
            FileTime ft = Files.getLastModifiedTime(path);
            if (cached.lastModified() != null && ft.compareTo(cached.lastModified()) <= 0) {
                return cached.kieBase();
            }
        }
        return reloadRules(path).kieBase();
    }

    /**
     * Vytvoří novou session.
     *
     * @param path
     * @return nová session
     * @throws IOException
     */
    public KieSession createKieSession(final Path path) throws IOException {
        logger.debug("Creating KieSession for rules: {}", path);
        return getKieBase(path).newKieSession();
    }

    public StatelessKieSession createKieStatelessSession(final Path path) throws IOException {
        logger.debug("Creating StatelessKieSession for rules: {}", path);

        StatelessKieSession ksession = getKieBase(path).newStatelessKieSession();
        if (logger.isTraceEnabled()) {
            ksession.addEventListener(new DefaultAgendaEventListener() {
                @Override
                public void afterMatchFired(AfterMatchFiredEvent event) {
                    logger.trace("Rule matched: {}", event.getMatch().getRule().getName());
                }
            });
        }
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
        rulStructureExtensionDefinitions.sort(
                Comparator.comparing(RulStructureExtensionDefinition::getPriority)
                          .thenComparing(RulStructureExtensionDefinition::getStructureExtensionDefinitionId));
    }
}
