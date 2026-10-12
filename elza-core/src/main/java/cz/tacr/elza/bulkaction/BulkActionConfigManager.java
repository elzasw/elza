package cz.tacr.elza.bulkaction;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import cz.tacr.elza.bulkaction.generator.ContentMigrationConfig;
import cz.tacr.elza.bulkaction.generator.DeleteItemConfig;
import cz.tacr.elza.bulkaction.generator.FundValidationConfig;
import cz.tacr.elza.bulkaction.generator.GenerateUnitIdConfig;
import cz.tacr.elza.bulkaction.generator.MoveDescItemConfig;
import cz.tacr.elza.bulkaction.generator.MultiActionConfig;
import cz.tacr.elza.bulkaction.generator.PersistentSortConfig;
import cz.tacr.elza.bulkaction.generator.SealUnitIdConfig;
import cz.tacr.elza.bulkaction.generator.SerialNumberConfig;
import cz.tacr.elza.bulkaction.generator.TestDataConfig;
import cz.tacr.elza.bulkaction.generator.multiple.AccessPointAggregationConfig;
import cz.tacr.elza.bulkaction.generator.multiple.CopyConfig;
import cz.tacr.elza.bulkaction.generator.multiple.DateRangeConfig;
import cz.tacr.elza.bulkaction.generator.multiple.ItemGeneratorConfig;
import cz.tacr.elza.bulkaction.generator.multiple.NodeCountConfig;
import cz.tacr.elza.bulkaction.generator.multiple.SizeCounterConfig;
import cz.tacr.elza.bulkaction.generator.multiple.StorageOrderGeneratorConfig;
import cz.tacr.elza.bulkaction.generator.multiple.TextAggregationConfig;
import cz.tacr.elza.bulkaction.generator.multiple.UnitCountConfig;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.repository.ActionRepository;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

/**
 * Manager konfigurace hromadných akcí.
 */
@Component
@Configuration
public class BulkActionConfigManager {

	private static final Logger logger = LoggerFactory.getLogger(BulkActionConfigManager.class);

    private final ActionRepository actionRepository;

    private final ResourcePathResolver resourcePathResolver;

    /**
     * Kinds of the actions by the configuration class: the YAML tag without "!". The kind is what the
     * client and the core key on (BulkActionVO.type, BulkAction.type of the REST API, the event
     * BULK_ACTION_STATE_CHANGE), never the code, which is the file name and unique per installation.
     * The values are the enum BulkActionType of the REST API (BulkActionKindsTest).
     */
    private static final Map<Class<? extends BaseActionConfig>, String> KINDS = createKinds();

    /**
     * Mapa konfigurací hromadných akcí.
     */
    private Map<String, BulkActionConfig> bulkActionConfigMap;

    @Autowired
    public BulkActionConfigManager(ActionRepository actionRepository, ResourcePathResolver resourcePathResolver) {
        this.actionRepository = actionRepository;
        this.resourcePathResolver = resourcePathResolver;
    }

    /**
     * Načtení hromadných akcí z adresáře.
     *
     * Function can is used also to reload configuration.
     *
     * @param resourcePathResolver
     */
    @Transactional(TxType.MANDATORY)
    public void load(ResourcePathResolver resourcePathResolver) {

        HashMap<String, BulkActionConfig> configs = new HashMap<>();

        List<RulAction> actions = actionRepository.findAll();

        Yaml yamlLoader = prepareYamlLoader();

        // load all bulk action configurations
        for (RulAction action : actions) {
            Path configFile = resourcePathResolver.getFunctionFile(action);
            String actionCode = action.getCode();

            BaseActionConfig config = loadActionConfig(actionCode, configFile, yamlLoader);
            configs.put(config.getCode(), config);
        }

        // publish configs
        this.bulkActionConfigMap = configs;
    }

    @Transactional(TxType.MANDATORY)
    public void load() {
        load(this.resourcePathResolver);
    }

    /**
     * Vyhledání nastavení hromadné akce podle kódu.
     *
     * @param code kód hromadné akce
     * @return nastavení hromadné akce
     */
    public BulkActionConfig get(final String code) {
        return bulkActionConfigMap.get(code);
    }

    /**
     * Kind of the action of the code ({@link BulkActionConfig#getType()}), null for an unknown code.
     */
    @Nullable
    public String getType(final String code) {
        BulkActionConfig config = bulkActionConfigMap.get(code);
        return config == null ? null : config.getType();
    }

    /**
     * Kind of the configuration class (its YAML tag without "!"), null for a class without a tag
     * (a broken action).
     */
    @Nullable
    public static String getKind(final Class<? extends BulkActionConfig> configClass) {
        return KINDS.get(configClass);
    }

    /**
     * All kinds of actions, in the order of registration.
     */
    public static Collection<String> getKinds() {
        return Collections.unmodifiableCollection(KINDS.values());
    }

    /**
     * Gets bulk action.
     *
     * @param code
     *            the code
     * @return the bulk action
     */
    @Bean
    @Scope("prototype")
    public BulkAction getBulkAction(final String code) {
        // get configuration object
        BulkActionConfig bac = bulkActionConfigMap.get(code);

        Validate.notNull(bac, "Failed to find action, code: %s", code);

        return bac.createBulkAction();
    }

    /**
     * Vrací seznam všech nastavení hromadných akcí.
     *
     * @return seznam nastavení hromadných akcí
     */
    public List<BulkActionConfig> getBulkActions() {
        return new ArrayList<>(bulkActionConfigMap.values());
    }

    /**
     * Load configuration of single action
     *
     * @param action
     * @param configFile
     * @param yamlLoader
     */
    private BaseActionConfig loadActionConfig(String actionCode, Path configFile, Yaml yamlLoader) {
        BaseActionConfig config = null;

        try (InputStream ios = Files.newInputStream(configFile, StandardOpenOption.READ)) {
            config = (BaseActionConfig) yamlLoader.load(ios);
        } catch (Exception e) {
            logger.error("Failed to initialize action, consider updating package with action, actionCode=" + actionCode, e);
            // on failure - log problem and create empty action
            config = new BrokenActionConfig(e);
    }

        config.setCode(actionCode);
        return config;
    }

    private static Map<Class<? extends BaseActionConfig>, String> createKinds() {
        Map<Class<? extends BaseActionConfig>, String> kinds = new LinkedHashMap<>();
        kinds.put(FundValidationConfig.class, "FundValidation");
        kinds.put(SerialNumberConfig.class, "SerialNumberGenerator");
        kinds.put(GenerateUnitIdConfig.class, "UnitIdGenerator");
        kinds.put(SealUnitIdConfig.class, "SealUnitId");
        kinds.put(TestDataConfig.class, "TestDataGenerator");
        kinds.put(MultiActionConfig.class, "MultiAction");
        kinds.put(MoveDescItemConfig.class, "MoveDescItem");
        kinds.put(ContentMigrationConfig.class, "ContentMigration");
        kinds.put(PersistentSortConfig.class, PersistentSortConfig.TYPE);
        return Collections.unmodifiableMap(kinds);
    }

    /**
     * Create yaml loader
     *
     * @return
     */
    private static Yaml prepareYamlLoader() {
        Constructor yamlCtor = new Constructor(new LoaderOptions());

        // Register type descriptors: the actions by their kinds
        KINDS.forEach((configClass, kind) -> yamlCtor.addTypeDescription(new TypeDescription(configClass, "!" + kind)));
        // the parts of !MultiAction, not actions of their own
        yamlCtor.addTypeDescription(new TypeDescription(ItemGeneratorConfig.class, "!ItemGenerator"));
        yamlCtor.addTypeDescription(new TypeDescription(StorageOrderGeneratorConfig.class, "!StorageOrderGenerator"));
        yamlCtor.addTypeDescription(new TypeDescription(DateRangeConfig.class, "!DateRange"));
        yamlCtor.addTypeDescription(new TypeDescription(TextAggregationConfig.class, "!TextAggregation"));
        yamlCtor.addTypeDescription(new TypeDescription(CopyConfig.class, "!Copy"));
        yamlCtor.addTypeDescription(new TypeDescription(NodeCountConfig.class, "!NodeCount"));
        yamlCtor.addTypeDescription(new TypeDescription(UnitCountConfig.class, "!UnitCount"));
        yamlCtor.addTypeDescription(new TypeDescription(DeleteItemConfig.class, "!DeleteItem"));
        yamlCtor.addTypeDescription(new TypeDescription(AccessPointAggregationConfig.class, "!AccessPointAggregation"));
        yamlCtor.addTypeDescription(new TypeDescription(SizeCounterConfig.class, "!SizeCounter"));
        return new Yaml(yamlCtor);
    }
}
