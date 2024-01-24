package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.packageimport.PackageService.ITEM_TYPE_XML;
import static cz.tacr.elza.packageimport.PackageService.ITEM_SPEC_XML;
import static cz.tacr.elza.packageimport.PackageService.PART_TYPE_XML;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.packageimport.PackageUtils;
import cz.tacr.elza.packageimport.autoimport.PackageInfoWrapper;
import cz.tacr.elza.packageimport.xml.ItemSpec;
import cz.tacr.elza.packageimport.xml.ItemSpecs;
import cz.tacr.elza.packageimport.xml.ItemType;
import cz.tacr.elza.packageimport.xml.ItemTypeAssign;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.packageimport.xml.PackageInfo;
import cz.tacr.elza.packageimport.xml.PartType;
import cz.tacr.elza.packageimport.xml.PartTypes;
import jakarta.annotation.PostConstruct;

/**
 * Místo pro načtení konfigurace pro Lucene indexu, před vlastní inicializací indexu.
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 */
@Component
public class IndexConfigurationReader {

    private static final Logger logger = LoggerFactory.getLogger(IndexConfigurationReader.class);

    //Toto NEFUNGUJE!! závislost na bean co je závislý na Hibernate
    //@Autowired
    //AeBatchRepository aeBatchRepository;

    //Toto je OK funguje
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String PACKAGE_XML = "package.xml";

    @Value("${elza.package.testing:false}")
    private Boolean testing;

    private List<PackageInfoWrapper> packagesToImport;
    private List<PackageInfoWrapper> allPackages;
    private List<String> partTypeCodes;
    private List<String> itemTypeCodes;
    private Map<String, List<String>> typeSpecMap;

    private Map<String, PackageInfoWrapper> latestVersionMap;

    @Value("${elza.workingDir}")
    private String workDir;

    @PostConstruct
    public void init() {
        packagesToImport = new ArrayList<>();
        partTypeCodes = new ArrayList<>();
        itemTypeCodes = new ArrayList<>();
        typeSpecMap = new HashMap<>();

        // get item type codes from DB
        List<Item> itemTypeItems = jdbcTemplate.query("SELECT * FROM rul_item_type", (rs, rowNum) -> new Item(rs.getInt("item_type_id"), rs.getString("code")));
        Map<Integer, String> itemTypeMap = itemTypeItems.stream().collect(Collectors.toMap(Item::getItemId, Item::getCode));
        itemTypeCodes.addAll(itemTypeItems.stream().map(i -> i.code).collect(Collectors.toList()));

        // get item spec codes from DB
        List<Item> itemSpecItems = jdbcTemplate.query("SELECT * FROM rul_item_spec", (rs, rowNum) -> new Item(rs.getInt("item_spec_id"), rs.getString("code")));
        Map<Integer, String> itemSpecMap = itemSpecItems.stream().collect(Collectors.toMap(Item::getItemId, Item::getCode));
        List<String> itemSpecCodes = itemSpecItems.stream().map(i -> i.getCode()).collect(Collectors.toList());

        // get item type spec assign
        List<Assign> typeSpecAssign = jdbcTemplate.query("SELECT * FROM rul_item_type_spec_assign", (rs, rowNum) -> new Assign(rs.getInt("item_type_id"), rs.getInt("item_spec_id")));

        // create map item type code -> item spec codes
        for (Assign item : typeSpecAssign) {
        	List<String> itemSpecs = typeSpecMap.computeIfAbsent(itemTypeMap.get(item.typeId), i -> new ArrayList<>());
        	itemSpecs.add(itemSpecMap.get(item.specId));
        }

        // get part type codes from DB
        List<String> partCodes = jdbcTemplate.query("SELECT * FROM rul_part_type", (rs, rowNum) -> rs.getString("code"));
        partTypeCodes.addAll(partCodes);

        Path dpkgDir = Paths.get(workDir, ResourcePathResolver.DPKG_DIR);
        if (!Files.exists(dpkgDir)) {
            return;
        }

        logger.info("Checking folder {} for packages...", dpkgDir);

        // get current packages from DB
        String packageSql = "SELECT * FROM rul_package";
        List<PackageInfo> packageInfoList = jdbcTemplate.query(packageSql, (rs, rowNum) -> {

            PackageInfo packageInfo = new PackageInfo();

            packageInfo.setCode(rs.getString("code"));
            packageInfo.setName(rs.getString("name"));
            packageInfo.setDescription(rs.getString("description"));
            packageInfo.setVersion(rs.getInt("version"));

            return packageInfo;
        });

        latestVersionMap = packageInfoList.stream()
                .collect(Collectors.toMap(PackageInfo::getCode, p -> new PackageInfoWrapper(p, null)));

        try (Stream<Path> streamPaths = Files.list(dpkgDir)) {

            // vyhledani poslednich verzi balicku
            for (Path path : streamPaths.collect(Collectors.toList())) {
                // check if file is package
                if (Files.isDirectory(path) || !path.getFileName().toString().endsWith("zip")) {
                    continue;
                }
                logger.info("Reading package info: {}", path);

                PackageInfoWrapper pkg = getPackageInfo(path);

                if (pkg == null) {
                    logger.error("Cannot read package info from file : {}. File is skipped.", path.toString());
                    continue;
                }

                PackageInfoWrapper mapPkg = latestVersionMap.get(pkg.getCode());
                // žádné informace o balíčku nebo nižší verzi
                if (mapPkg == null || mapPkg.getVersion() < pkg.getVersion() || (testing && mapPkg.getVersion() <= pkg.getVersion())) {
                    packagesToImport.add(new PackageInfoWrapper(pkg.getPkg(), path));
                    latestVersionMap.put(pkg.getCode(), new PackageInfoWrapper(pkg.getPkg(), path));

                    Map<String, ByteArrayInputStream> streamMap = PackageUtils.createStreamsMap(pkg.getPath().toFile());

                    ItemTypes itemTypes = PackageUtils.convertXmlStreamToObject(ItemTypes.class, streamMap.get(ITEM_TYPE_XML));
                    for (ItemType itemType : itemTypes.getItemTypes()) {
                        if (!itemTypeCodes.contains(itemType.getCode())) {
                            itemTypeCodes.add(itemType.getCode());
                        }
                    }

                    ItemSpecs itemSpecs = PackageUtils.convertXmlStreamToObject(ItemSpecs.class, streamMap.get(ITEM_SPEC_XML));
                    for (ItemSpec itemSpec : itemSpecs.getItemSpecs()) {
                    	if (!itemSpecCodes.contains(itemSpec.getCode())) {
                    		itemSpecCodes.add(itemSpec.getCode());
                    		for (ItemTypeAssign itemTypeAssign : itemSpec.getItemTypeAssigns()) {
                    			List<String> listItemSpecCodes = typeSpecMap.computeIfAbsent(itemTypeAssign.getCode(), i -> new ArrayList<>());
                    			listItemSpecCodes.add(itemSpec.getCode());
                    		}
                    	}
                    }

                    PartTypes partTypes = PackageUtils.convertXmlStreamToObject(PartTypes.class, streamMap.get(PART_TYPE_XML));
                    if (partTypes != null) {
	                    for (PartType partType : partTypes.getPartTypes()) {
	                        if (!partTypeCodes.contains(partType.getCode())) {
	                            partTypeCodes.add(partType.getCode());
	                        }
	                    }
                    }
                } else {
                    throw new IllegalStateException("Package is an older version than the one already imported. New package version: "
                            + pkg.getVersion() + ", old package version: " + mapPkg.getVersion());
                }
            }
            allPackages = new ArrayList<>(latestVersionMap.values());

        } catch (IOException e) {
            logger.error("Error processing a package zip file.", e);
            throw new SystemException("Error processing a package zip file.", e);
        }
    }

    public List<String> getPartTypeCodes() {
        return partTypeCodes;
    }

    public List<String> getItemTypeCodes() {
        return itemTypeCodes;
    }

    public List<String> getItemSpecCodesByTypeCode(String itemTypeCode) {
		return typeSpecMap.getOrDefault(itemTypeCode, new ArrayList<>());
	}

	public List<PackageInfoWrapper> getPackagesToImport() {
        return packagesToImport;
    }

    public List<PackageInfoWrapper> getAllPackages() {
        return allPackages;
    }

    private PackageInfoWrapper getPackageInfo(Path path) throws IOException {
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            ZipEntry zipEntry = zipFile.getEntry(PACKAGE_XML);
            if (zipEntry == null) {
                // package info not found
                return null;
            }
            try (InputStream is = zipFile.getInputStream(zipEntry)) {
                ByteArrayInputStream bais = new ByteArrayInputStream(IOUtils.toByteArray(is));
                PackageInfo pkgZip = PackageUtils.convertXmlStreamToObject(PackageInfo.class, bais);

                return new PackageInfoWrapper(pkgZip, path);
            }
        }
    }

    private class Item { // for item_type & item_spec
    	Integer itemId;
    	String code;
		Item(Integer itemId, String code) {
			this.itemId = itemId;
			this.code = code;
		}
		public Integer getItemId() {
			return itemId;
		}
		public String getCode() {
			return code;
		}
    }
    
    private class Assign { // for assigning typeId & specId
    	Integer typeId;
    	Integer specId;
		Assign(Integer typeId, Integer specId) {
			super();
			this.typeId = typeId;
			this.specId = specId;
		}
    }
}
