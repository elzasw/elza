package cz.tacr.elza.bulkaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.RulAction;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.ActionRepository;
import cz.tacr.elza.utils.Yaml;
import liquibase.util.file.FilenameUtils;


/**
 * Manager konfigurace hromadných akcí.
 */
@Component
public class BulkActionConfigManager {

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
            String filePath = getFunctionsDir(action.getPackage().getCode()) + File.separator + action.getFilename();
            files[i] = new File(filePath);
            if (!files[i].exists()) {
                throw new SystemException("Soubor neexistuje: " + filePath);
            }
        }

        for (File file : files) {
            try {
                InputStream ios = new FileInputStream(file);
                BulkActionConfig bulkActionConfig = new BulkActionConfig();
                bulkActionConfig.setCode(FilenameUtils.getBaseName(file.getName()));
                bulkActionConfig.getYaml().load(ios);
                ios.close();
                bulkActionConfigMap.put(bulkActionConfig.getCode(), bulkActionConfig);
            } catch(Yaml.YAMLInvalidContentException ye) {
                throw new IllegalArgumentException("Failed to read configuration file: "+file.getAbsolutePath(), ye);
            }
        }

    }

    /**
     * Zkopíruje (+nahradí) výchozí nastavení hromadných akcí.
     *
     * @param dir složka pro uložení
     */
    private void copyDefaultFromResources(final File dir) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File defaultDir = new File(classLoader.getResource("bulkactions").getFile());
        File[] files = defaultDir.listFiles((dir1, name) -> name.endsWith(extension));
        if (files != null) {
            for (File file : files) {
                File fileCopy = new File(
                        dir.toString() + File.separator + FilenameUtils.getBaseName(file.getName()) + extension);
                Files.copy(file.toPath(), fileCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
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
     * Vložení nastavení hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    public void put(final BulkActionConfig bulkActionConfig) {
        bulkActionConfigMap.put(bulkActionConfig.getCode(), bulkActionConfig);
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
     * Vrací cestu k souboru podle nastavení hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     * @return cesta k souboru
     */
    private String getFileName(final BulkActionConfig bulkActionConfig) {
        return rulesDir + File.separator + bulkActionConfig.getCode() + extension;
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
