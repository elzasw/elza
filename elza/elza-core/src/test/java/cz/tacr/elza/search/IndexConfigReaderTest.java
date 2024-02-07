package cz.tacr.elza.search;

import static cz.tacr.elza.packageimport.PackageService.ITEM_TYPE_XML;
import static cz.tacr.elza.packageimport.PackageService.ITEM_SPEC_XML;
import static cz.tacr.elza.packageimport.PackageService.PART_TYPE_XML;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.bridge.IndexConfigReader;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.packageimport.PackageUtils;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemTypeAssign;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.packageimport.xml.PartTypes;
import jakarta.annotation.PostConstruct;

/**
 * Místo pro načtení konfigurace pro Lucene indexu, před vlastní inicializací indexu.
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 */
@Primary
@Component
public class IndexConfigReaderTest implements IndexConfigReader {

    private static final Logger logger = LoggerFactory.getLogger(IndexConfigReaderTest.class);

    private static final String PACKAGE_XML = "package.xml";

    private static final String TARGET_TEST_CLASSES = "target/test-classes";

    @Value("${elza.package.testing:false}")
    private Boolean testing;

    private List<String> partTypeCodes;
    private List<String> itemTypeCodes;
    private Map<String, List<String>> typeSpecMap;

    @PostConstruct
    public void init() {
        partTypeCodes = new ArrayList<>();
        itemTypeCodes = new ArrayList<>();
        typeSpecMap = new HashMap<>();

    	logger.info("Checking folder {} for packages...", Paths.get(TARGET_TEST_CLASSES));

    	try (Stream<Path> streamPaths = Files.list(Paths.get(TARGET_TEST_CLASSES))) {

    		for (Path path : streamPaths.collect(Collectors.toList())) {
    			// check is directory and contains xml files of package
    			if (Files.isDirectory(path) && Files.exists(path.resolve(PACKAGE_XML))) {

    				logger.info("Reading package info: {}", path);

                    PartTypes partTypes = PackageUtils.convertXmlFileToObject(PartTypes.class, path.resolve(PART_TYPE_XML));
                    if (partTypes != null) {
                    	partTypeCodes.addAll(partTypes.getPartTypes().stream().map(i -> i.getCode()).collect(Collectors.toList()));
                    }

                    ItemTypes itemTypes = PackageUtils.convertXmlFileToObject(ItemTypes.class, path.resolve(ITEM_TYPE_XML));
                    if (itemTypes != null) {
                    	itemTypeCodes.addAll(itemTypes.getItemTypes().stream().map(i -> i.getCode()).collect(Collectors.toList()));
                    }

                    ItemSpecs itemSpecs = PackageUtils.convertXmlFileToObject(ItemSpecs.class, path.resolve(ITEM_SPEC_XML));
                    if (itemSpecs != null) {
                    	itemSpecs.getItemSpecs().forEach(itemSpec -> {
                    		for (ItemTypeAssign itemTypeAssign : itemSpec.getItemTypeAssigns()) {
                    			List<String> listItemSpecCodes = typeSpecMap.computeIfAbsent(itemTypeAssign.getCode(), i -> new ArrayList<>());
                    			listItemSpecCodes.add(itemSpec.getCode());
                    		}
                    	});
                    }
    			}
    		}

        } catch (IOException e) {
            logger.error("Error processing package dir with xml files.", e);
            throw new SystemException("Error processing package dir with xml files.", e);
        }
    }

    @Override
    public List<String> getPartTypeCodes() {
        return partTypeCodes;
    }

    @Override
    public List<String> getItemTypeCodes() {
        return itemTypeCodes;
    }

    @Override
    public List<String> getItemSpecCodesByTypeCode(String itemTypeCode) {
		return typeSpecMap.getOrDefault(itemTypeCode, new ArrayList<>());
	}
}