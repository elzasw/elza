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

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cz.tacr.elza.utils.Yaml;
import liquibase.util.file.FilenameUtils;


/**
 * Manager konfigurace hromadných akcí.
 *
 * @author Martin Šlapa
 * @author Petr Pytelka
 * @since 10.11.2015
 */
@Component
public class BulkActionConfigManager {

    /**
     * Cesta adresáře pro konfiguraci hromadných akcí.
     */
    @Value("${elza.bulkactions.bulkactionsDir}")
    private String path;

    /**
     * Podporovaný formát souborů pro konfiguraci - použité pro ukládání nových a vyhledání v adresáři.
     */
    private String extension = ".yaml";

    /**
     * Mapa konfigurací hromadných akcí.
     */
    private Map<String, BulkActionConfig> bulkActionConfigMap = new HashMap<>();

    /**
     * @return cesta
     */
    public String getPath() {
        return path;
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
    public void load() throws IOException {

        bulkActionConfigMap = new HashMap<>();

        File dir = new File(path);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        //copyDefaultFromResources(dir);

        // vyhledání souborů v adresáři
        File[] files = dir.listFiles((dir1, name) -> name.endsWith(extension));

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
     * Uložení nastavení hromadných akcí do konfuguračních souborů.
     */
    public void save() throws IOException, Yaml.YAMLNotInitializedException {
        for (Map.Entry<String, BulkActionConfig> bulkActionConfigEntry : bulkActionConfigMap.entrySet()) {
            String name = getFileName(bulkActionConfigEntry.getValue());
            saveFile(bulkActionConfigEntry.getValue().getYaml(), name);
        }
    }

    /**
     * Aktualizace nastavení hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     * @return upravené nastavení hromadné akce
     */
    public BulkActionConfig update(final BulkActionConfig bulkActionConfig) throws IOException, Yaml.YAMLNotInitializedException {
        BulkActionConfig bulkActionConfigOrig = get(bulkActionConfig.getCode());

        if (bulkActionConfigOrig == null) {
            throw new SystemException("Hromadná akce '" + bulkActionConfig.getCode() + "' neexistuje!", BaseCode.ID_NOT_EXIST);
        }

        // uložení do souboru
        save(bulkActionConfig);

        // uložení do paměti
        put(bulkActionConfig);

        return bulkActionConfig;

    }

    /**
     * Smazání nastavení hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    public void delete(final BulkActionConfig bulkActionConfig) {
        BulkActionConfig bulkActionConfigOrig = get(bulkActionConfig.getCode());
        if (bulkActionConfigOrig == null) {
            throw new SystemException("Hromadná akce '" + bulkActionConfig.getCode() + "' neexistuje!", BaseCode.ID_NOT_EXIST);
        }
        String name = getFileName(bulkActionConfigOrig);
        File file = new File(name);

        if (!file.exists()) {
            throw new SystemException("Soubor hromadné akce neexistuje!");
        }

        file.delete();
        bulkActionConfigMap.remove(bulkActionConfig.getCode());
    }

    /**
     * Uložení nastavení hromadné akce do souboru.
     *  @param yaml struktura kofiguračního souboru
     * @param name název konfiguračního souboru
     */
    private void saveFile(final Yaml yaml, final String name) throws IOException, Yaml.YAMLNotInitializedException {
        File file = new File(name);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        yaml.save(file);
    }

    /**
     * Uložení nastavení hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    public void save(final BulkActionConfig bulkActionConfig) throws IOException, Yaml.YAMLNotInitializedException {
        String name = getFileName(bulkActionConfig);
        saveFile(bulkActionConfig.getYaml(), name);
    }

    /**
     * Vrací cestu k souboru podle nastavení hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     * @return cesta k souboru
     */
    private String getFileName(final BulkActionConfig bulkActionConfig) {
        return path + File.separator + bulkActionConfig.getCode() + extension;
    }

}
