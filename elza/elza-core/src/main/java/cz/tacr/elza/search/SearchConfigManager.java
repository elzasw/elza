package cz.tacr.elza.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;

@Component
public class SearchConfigManager {

    private ElzaSearchConfig elzaSearchConfig;

    private static final Logger logger = LoggerFactory.getLogger(SearchConfigManager.class);

    private final String FILE_NAME = "config/indexSearch.yaml";

    private static boolean configException = true;

    private Long lastUpdate = 0L;

    @Scheduled(fixedRate = 10000)
    public void load() {
        try {
            File file = new File(FILE_NAME);
            FileSystemResource fileSystemResource = new FileSystemResource(file);
            final long lastModified = fileSystemResource.lastModified();
            if (lastModified > lastUpdate) {
                lastUpdate = lastModified;
                try (InputStream is = fileSystemResource.getInputStream();
                     Reader reader = new UnicodeReader(is)) {
                    Yaml yaml = new Yaml();
                    ElzaSearchYaml prefixClientConfig = yaml.loadAs(reader, ElzaSearchYaml.class);
                    this.elzaSearchConfig = prefixClientConfig.getSearch();
                    logger.info("Konfigurace vyhledávání načtena.");
                    configException = true;
                }
            }

        } catch (Exception e) {
            if (configException) {
                configException = false;
                logger.info("Nenalezen soubor s konfigurací vyhledávání indexů v elza-web/config/indexSearch.yaml");
            }
        }
    }

    public ElzaSearchConfig getElzaSearchConfig() {
        return elzaSearchConfig;
    }
}
