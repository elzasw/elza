package cz.tacr.elza.bulkaction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import cz.tacr.elza.bulkaction.generator.FundValidationConfig;
import cz.tacr.elza.bulkaction.generator.MoveDescItemConfig;
import cz.tacr.elza.bulkaction.generator.MultiActionConfig;
import cz.tacr.elza.bulkaction.generator.SerialNumberConfig;
import cz.tacr.elza.bulkaction.generator.TestDataConfig;
import cz.tacr.elza.bulkaction.generator.UnitIdConfig;
import cz.tacr.elza.bulkaction.generator.multiple.CopyConfig;
import cz.tacr.elza.bulkaction.generator.multiple.DateRangeConfig;
import cz.tacr.elza.bulkaction.generator.multiple.NodeCountConfig;
import cz.tacr.elza.bulkaction.generator.multiple.TextAggregationConfig;
import cz.tacr.elza.bulkaction.generator.multiple.UnitCountConfig;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.ActionRepository;
import liquibase.util.file.FilenameUtils;


/**
 * Manager konfigurace hromadných akcí.
 */
@Component
public class BulkActionConfigManager {

	private static final Logger logger = LoggerFactory.getLogger(BulkActionConfigManager.class);

	@Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private ActionRepository actionRepository;

    /**
     * Mapa konfigurací hromadných akcí.
     */
    private Map<String, BulkActionConfig> bulkActionConfigMap = new HashMap<>();

    /**
     * Načtení hromadných akcí z adresáře.
     */
    @Transactional
    public void load() throws IOException {

        bulkActionConfigMap = new HashMap<>();

        List<RulAction> actions = actionRepository.findAll();

        // vyhledání souborů v adresáři
        List<Path> actionFiles = new ArrayList<>(actions.size());

        for (RulAction action : actions) {
            Path file = resourcePathResolver.getFunctionFile(action);
            if (!Files.exists(file)) {
                throw new SystemException("Action file not found, path: " + file);
            }
            actionFiles.add(file);
        }

		// prepare yaml loader
		Constructor yamlCons = new Constructor();
		yamlCons.addTypeDescription(new TypeDescription(FundValidationConfig.class, "!FundValidation"));
		yamlCons.addTypeDescription(new TypeDescription(SerialNumberConfig.class, "!SerialNumberGenerator"));
		yamlCons.addTypeDescription(new TypeDescription(UnitIdConfig.class, "!UnitIdGenerator"));
		yamlCons.addTypeDescription(new TypeDescription(TestDataConfig.class, "!TestDataGenerator"));
		yamlCons.addTypeDescription(new TypeDescription(MultiActionConfig.class, "!MultiAction"));
		yamlCons.addTypeDescription(new TypeDescription(DateRangeConfig.class, "!DateRange"));
		yamlCons.addTypeDescription(new TypeDescription(TextAggregationConfig.class, "!TextAggregation"));
		yamlCons.addTypeDescription(new TypeDescription(CopyConfig.class, "!Copy"));
		yamlCons.addTypeDescription(new TypeDescription(NodeCountConfig.class, "!NodeCount"));
		yamlCons.addTypeDescription(new TypeDescription(UnitCountConfig.class, "!UnitCount"));
		yamlCons.addTypeDescription(new TypeDescription(MoveDescItemConfig.class, "!MoveDescItem"));
		Yaml yamlLoader = new Yaml(yamlCons);

		// load files
        for (Path file : actionFiles) {
			// load bulk action
			String actionCode = FilenameUtils.getBaseName(file.getFileName().toString());

			BulkActionConfig bulkActionConfig = null;
			try (InputStream ios = Files.newInputStream(file, StandardOpenOption.READ)) {
				bulkActionConfig = (BulkActionConfig) yamlLoader.load(ios);
			} catch (Exception e) {
                logger.error("Failed to initialize action, consider updating package with action, actionCode=" + actionCode, e);
				// on failure - log problem and create empty action
				bulkActionConfig = new BrokenActionConfig(e);
			}
			bulkActionConfig.setCode(actionCode);
			bulkActionConfigMap.put(actionCode, bulkActionConfig);
        }

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
     * Vrací seznam všech nastavení hromadných akcí.
     *
     * @return seznam nastavení hromadných akcí
     */
    public List<BulkActionConfig> getBulkActions() {
        return new ArrayList<>(bulkActionConfigMap.values());
    }
}
