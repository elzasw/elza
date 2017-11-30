package cz.tacr.elza.bulkaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    /**
     * Název složky v pravidlech.
     */
    public final static String FOLDER = "functions";

    /**
     * Cesta adresáře pro konfiguraci pravidel.
     */
    @Value("${elza.rulesDir}")
    private String rulesDir;

    @Autowired
    private ActionRepository actionRepository;

    /**
     * Podporovaný formát souborů pro konfiguraci - použité pro ukládání nových a vyhledání v adresáři.
     */
    private String extension = ".yaml";

    /**
     * Mapa konfigurací hromadných akcí.
     */
    private Map<String, BulkActionConfig> bulkActionConfigMap = new HashMap<>();

    /**
     * @return cesta k pravidlům
     */
    public String getRulesDir() {
        return rulesDir;
    }

    /**
     * @return přípona
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Načtení hromadných akcí z adresáře.
     */
    @Transactional
    public void load() throws IOException {

        bulkActionConfigMap = new HashMap<>();

        File dir = new File(rulesDir);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        List<RulAction> actions = actionRepository.findAll();

        // vyhledání souborů v adresáři
        File[] files = new File[actions.size()];

        for (int i = 0; i < actions.size(); i++) {
            RulAction action = actions.get(i);
            String filePath = getFunctionsDir(action.getRuleSet().getCode()) + File.separator + action.getFilename();
            files[i] = new File(filePath);
            if (!files[i].exists()) {
                throw new SystemException("Soubor neexistuje: " + filePath);
            }
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
        for (File file : files) {
			// load bulk action
			String actionCode = FilenameUtils.getBaseName(file.getName());

			BulkActionConfig bulkActionConfig = null;
			try (InputStream ios = new FileInputStream(file)) {
				bulkActionConfig = (BulkActionConfig) yamlLoader.load(ios);
			} catch (Exception e) {
				logger.error(
				        "Failed to initialize action, consider updating package with action, actionCode=" + actionCode,
				        e);
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

    /**
     * Vrací úplnou cestu k adresáři funkcí podle balíčku.
     *
     * @param code kód balíčku (pravidel)
     * @return cesta k adresáři funkcí
     */
    public String getFunctionsDir(final String code) {
        return rulesDir + File.separator + code + File.separator + FOLDER;
    }
}
