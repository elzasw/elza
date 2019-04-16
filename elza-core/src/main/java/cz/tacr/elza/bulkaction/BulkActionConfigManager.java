package cz.tacr.elza.bulkaction;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import cz.tacr.elza.bulkaction.generator.DeleteItemConfig;
import cz.tacr.elza.bulkaction.generator.FundValidationConfig;
import cz.tacr.elza.bulkaction.generator.MoveDescItemConfig;
import cz.tacr.elza.bulkaction.generator.MultiActionConfig;
import cz.tacr.elza.bulkaction.generator.PersistentSortConfig;
import cz.tacr.elza.bulkaction.generator.SealUnitIdConfig;
import cz.tacr.elza.bulkaction.generator.SerialNumberConfig;
import cz.tacr.elza.bulkaction.generator.TestDataConfig;
import cz.tacr.elza.bulkaction.generator.GenerateUnitIdConfig;
import cz.tacr.elza.bulkaction.generator.multiple.CopyConfig;
import cz.tacr.elza.bulkaction.generator.multiple.DateRangeConfig;
import cz.tacr.elza.bulkaction.generator.multiple.ItemGeneratorConfig;
import cz.tacr.elza.bulkaction.generator.multiple.NodeCountConfig;
import cz.tacr.elza.bulkaction.generator.multiple.TextAggregationConfig;
import cz.tacr.elza.bulkaction.generator.multiple.UnitCountConfig;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.repository.ActionRepository;

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

    /**
     * Create yaml loader
     *
     * @return
     */
    private static Yaml prepareYamlLoader() {
        Constructor yamlCtor = new Constructor();

        // Register type descriptors
        yamlCtor.addTypeDescription(new TypeDescription(FundValidationConfig.class, "!FundValidation"));
        yamlCtor.addTypeDescription(new TypeDescription(ItemGeneratorConfig.class, "!ItemGenerator"));
        yamlCtor.addTypeDescription(new TypeDescription(SerialNumberConfig.class, "!SerialNumberGenerator"));
        yamlCtor.addTypeDescription(new TypeDescription(GenerateUnitIdConfig.class, "!UnitIdGenerator"));
        yamlCtor.addTypeDescription(new TypeDescription(SealUnitIdConfig.class, "!SealUnitId"));
        yamlCtor.addTypeDescription(new TypeDescription(TestDataConfig.class, "!TestDataGenerator"));
        yamlCtor.addTypeDescription(new TypeDescription(MultiActionConfig.class, "!MultiAction"));
        yamlCtor.addTypeDescription(new TypeDescription(DateRangeConfig.class, "!DateRange"));
        yamlCtor.addTypeDescription(new TypeDescription(TextAggregationConfig.class, "!TextAggregation"));
        yamlCtor.addTypeDescription(new TypeDescription(CopyConfig.class, "!Copy"));
        yamlCtor.addTypeDescription(new TypeDescription(NodeCountConfig.class, "!NodeCount"));
        yamlCtor.addTypeDescription(new TypeDescription(UnitCountConfig.class, "!UnitCount"));
        yamlCtor.addTypeDescription(new TypeDescription(MoveDescItemConfig.class, "!MoveDescItem"));
        yamlCtor.addTypeDescription(new TypeDescription(PersistentSortConfig.class, "!PersistentSort"));
        yamlCtor.addTypeDescription(new TypeDescription(DeleteItemConfig.class, "!DeleteItem"));

        return new Yaml(yamlCtor);
    }
}
