package cz.tacr.elza.bulkaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.yaml.YamlProperties;
import liquibase.util.file.FilenameUtils;


/**
 * Manager konfigurace hromadných akcí.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
@Component
public class BulkActionConfigManager {

    /**
     * Cesta adresáře pro konfiguraci hromadných akcí.
     * TODO: napojit na spring konfiguraci
     */
    private String path = "./bulkactions";

    /**
     * Podporovaný formát souborů pro konfiguraci - použité pro ukládání nových a vyhledání v adresáři.
     */
    private String extension = ".yaml";

    /**
     * Mapa konfigurací hromadných akcí.
     */
    private Map<String, BulkActionConfig> bulkActionConfigMap = new HashMap<>();

    /**
     * Načtení hromadných akcí z adresáře.
     */
    public void load() throws IOException {

        bulkActionConfigMap = new HashMap<>();

        File dir = new File(path);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        // vyhledání souborů v adresáři
        File[] files = dir.listFiles((dir1, name) -> name.endsWith(extension));

        for (File file : files) {
            InputStream ios = new FileInputStream(file);
            BulkActionConfig bulkActionConfig = new BulkActionConfig();
            bulkActionConfig.setCode(FilenameUtils.getBaseName(file.getName()));
            bulkActionConfig.getYaml().load(ios);
            ios.close();
            bulkActionConfigMap.put(bulkActionConfig.getCode(), bulkActionConfig);
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
    public void save() throws IOException {
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
    public BulkActionConfig update(final BulkActionConfig bulkActionConfig) throws IOException {
        BulkActionConfig bulkActionConfigOrig = get(bulkActionConfig.getCode());

        if (bulkActionConfigOrig == null) {
            throw new IllegalArgumentException("Hromadná akce neexistuje!");
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
            throw new IllegalArgumentException("Hromadná akce neexistuje!");
        }
        String name = getFileName(bulkActionConfigOrig);
        File file = new File(name);

        if (!file.exists()) {
            throw new IllegalArgumentException("Soubor hromadné akce neexistuje!");
        }

        file.delete();
    }

    /**
     * Uložení nastavení hromadné akce do souboru.
     *
     * @param yaml struktura kofiguračního souboru
     * @param name název konfiguračního souboru
     */
    private void saveFile(final YamlProperties yaml, final String name) throws IOException {
        File file = new File(name);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file);
        try {
            yaml.store(fw, "");
        } finally {
            fw.close();
        }
    }

    /**
     * Uložení nastavení hromadné akce.
     *
     * @param bulkActionConfig nastavení hromadné akce
     */
    public void save(final BulkActionConfig bulkActionConfig) throws IOException {
        String name = getFileName(bulkActionConfig);
        saveFile(bulkActionConfig.getYaml(), name);
    }

    /**
     * Vrací cestu k souboru podle nastavení hromadné akce.
     * @param bulkActionConfig  nastavení hromadné akce
     * @return cesta k souboru
     */
    private String getFileName(final BulkActionConfig bulkActionConfig) {
        return path + File.separator + bulkActionConfig.getCode() + extension;
    }

}
