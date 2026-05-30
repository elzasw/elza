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
import java.util.Collection;
import java.util.Collections;
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
import org.springframework.util.FileSystemUtils;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.DataType;
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
 */
@Component
public class IndexConfigReaderImpl implements IndexConfigReader {

    private static final Logger logger = LoggerFactory.getLogger(IndexConfigReaderImpl.class);

    //Toto NEFUNGUJE!! závislost na bean co je závislý na Hibernate
    //@Autowired
    //AeBatchRepository aeBatchRepository;

    //Toto je OK funguje
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String PACKAGE_XML = "package.xml";

    private static final String SELECT_RUL_PACKAGE = "SELECT * FROM rul_package";

    private static final String SELECT_RUL_DATA_TYPE = "SELECT * FROM rul_data_type";

    private static final String SELECT_RUL_ITEM_TYPE = "SELECT * FROM rul_item_type";

    private static final String SELECT_RUL_ITEM_SPEC = "SELECT * FROM rul_item_spec";

    private static final String SELECT_RUL_ITEM_TYPE_SPEC = "SELECT * FROM rul_item_type_spec_assign";

    private static final String SELECT_RUL_PART_TYPE = "SELECT * FROM rul_part_type";

    private static final String DATA_TYPE_ID = "data_type_id";

    private static final String CODE = "code";

    private static final String ITEM_TYPE_ID = "item_type_id";
    
    private static final String ITEM_SPEC_ID = "item_spec_id";

    @Value("${elza.package.testing:false}")
    private Boolean testing;

    private List<PackageInfoWrapper> packagesToImport;
    private List<PackageInfoWrapper> allPackages;

    private List<String> partTypeCodes;
    private List<String> itemSpecCodes;
    private Map<String, DataType> itemTypeDataTypeMap;
    private Map<String, ItemTypeInfo> itemTypeMap;

    private Map<String, PackageInfoWrapper> latestVersionMap;

    @Value("${elza.workingDir}")
    private String workDir;

    public static boolean cleanIndexDir = false;

    @PostConstruct
    public void init() throws IOException {
        packagesToImport = new ArrayList<>();
        partTypeCodes = new ArrayList<>();
        itemTypeDataTypeMap = new HashMap<>();
        itemSpecCodes = new ArrayList<>();
        itemTypeMap = new HashMap<>();

        // vyčištění složek s indexovými soubory
        if (cleanIndexDir) {
            Path luceneIndexesDir = Paths.get(workDir, ResourcePathResolver.LUCENE_DIR, ResourcePathResolver.INDEXES_DIR);
            FileSystemUtils.deleteRecursively(luceneIndexesDir);
        }

        // get current packages from db
        List<PackageInfo> packageInfoList = jdbcTemplate.query(SELECT_RUL_PACKAGE, (rs, rowNum) -> {
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.setId(rs.getInt("package_id"));
            packageInfo.setCode(rs.getString("code"));
            packageInfo.setVersion(rs.getInt("version"));
            return packageInfo;
        });

        // reading data from xml packages files
        Path dpkgDir = Paths.get(workDir, ResourcePathResolver.DPKG_DIR);
        if (Files.exists(dpkgDir)) {

            logger.info("Checking folder {} for packages...", dpkgDir);

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
                    boolean readFromFile = true;
                    if (mapPkg != null) {
                        // mame data z db
                        if (mapPkg.getVersion() > pkg.getVersion()) {
                            throw new IllegalStateException("Package is an older version than the one already imported. New package version: "
                                    + pkg.getVersion() + ", old package version: " + mapPkg.getVersion());
                        }
                        // verze v databázi a v zip archivu jsou stejné i to není vývoj
                        if (mapPkg.getVersion().equals(pkg.getVersion()) && !testing) {
                            readFromFile = false;
                        }
                    }

                    // pokud balíček není stažen nebo jeho verze neodpovídá stažené (menší) nebo probíhá vývoj
                    if (readFromFile) {
                        packagesToImport.add(new PackageInfoWrapper(pkg.getPkg(), path));
                        latestVersionMap.put(pkg.getCode(), new PackageInfoWrapper(pkg.getPkg(), path));

                        Map<String, ByteArrayInputStream> streamMap = PackageUtils.createStreamsMap(pkg.getPath().toFile());
                        readTypeAndSpecDataFromZipFilePackage(streamMap);
                    }
                }
                allPackages = new ArrayList<>(latestVersionMap.values());

            } catch (IOException e) {
                logger.error("Error processing a package zip file.", e);
                throw new SystemException("Error processing a package zip file.", e);
            }
        } else {
            // Fallback initialization if dpkg directory does not exist, e.g. for tests or first run without packages
            allPackages = Collections.emptyList();
        }

        // Merge data from DB across ALL packages — always, even when dpkgDir is absent
        // (e.g. tests or first run without packages), so that DB-defined types/specs/parts
        // are still registered in the index schema.
        // ZIP contributions are preserved (authoritative for newly added types/specs not yet in DB);
        // DB fills in cross-package spec assignments and any specs/types only present in DB
        // (e.g. customer-specific specs added without refreshing the package XML).
        mergeTypeAndSpecDataFromDb();
    }

    private void mergeTypeAndSpecDataFromDb() {
        // dataType lookup
        Map<Integer, DataType> dataTypeMap = new HashMap<>();
        jdbcTemplate.query(SELECT_RUL_DATA_TYPE, (rs, rowNum) -> dataTypeMap.put(rs.getInt(DATA_TYPE_ID), DataType.fromCode(rs.getString(CODE))));

        // all item types from DB (no package filter — needed to resolve cross-package spec assignments)
        List<RulItemType> itemTypeItems = jdbcTemplate.query(SELECT_RUL_ITEM_TYPE, (rs, rowNum) -> new RulItemType(rs.getInt(ITEM_TYPE_ID), rs.getInt(DATA_TYPE_ID), rs.getString(CODE)));
        Map<Integer, String> itemTypeCodeById = itemTypeItems.stream()
                .collect(Collectors.toMap(RulItemType::getId, RulItemType::getCode));

        // all item specs from DB
        List<RulItemSpec> itemSpecItems = jdbcTemplate.query(SELECT_RUL_ITEM_SPEC, (rs, rowNum) -> new RulItemSpec(rs.getInt(ITEM_SPEC_ID), rs.getString(CODE)));
        Map<Integer, String> itemSpecCodeById = itemSpecItems.stream()
                .collect(Collectors.toMap(RulItemSpec::getId, RulItemSpec::getCode));
        for (RulItemSpec spec : itemSpecItems) {
            if (!itemSpecCodes.contains(spec.getCode())) {
                itemSpecCodes.add(spec.getCode());
            }
        }

        // ensure every DB itemType is present in itemTypeMap.
        // ZIP-loaded entries are preserved — for an upgraded package the ZIP carries the
        // authoritative DataType for any newly added itemType not yet in DB.
        for (RulItemType itemType : itemTypeItems) {
            if (!itemTypeMap.containsKey(itemType.getCode())) {
                ItemTypeInfo info = new ItemTypeInfo(itemType.getId(), itemType.getCode(),
                                                    dataTypeMap.get(itemType.getDataTypeId()));
                itemTypeMap.put(itemType.getCode(), info);
            }
        }

        // merge all type-spec assignments. rul_item_type_spec_assign has no package_id,
        // so a single pass over all rows covers cross-package assignments and any specs
        // that exist only in DB (e.g. customer extensions). Dedup via .contains() avoids
        // duplicate Lucene field registration when ZIP and DB list the same assignment.
        List<RulItemTypeSpecAssign> typeSpecAssign = jdbcTemplate.query(SELECT_RUL_ITEM_TYPE_SPEC, (rs, rowNum) -> new RulItemTypeSpecAssign(rs.getInt(ITEM_TYPE_ID), rs.getInt(ITEM_SPEC_ID)));
        for (RulItemTypeSpecAssign assign : typeSpecAssign) {
            String typeCode = itemTypeCodeById.get(assign.getTypeId());
            String specCode = itemSpecCodeById.get(assign.getSpecId());
            if (typeCode == null || specCode == null) {
                continue;
            }
            ItemTypeInfo info = itemTypeMap.get(typeCode);
            if (info == null) {
                continue;
            }
            List<String> specs = info.getSpecs();
            if (!specs.contains(specCode)) {
                specs.add(specCode);
            }
        }

        // part type codes
        List<String> partCodes = jdbcTemplate.query(SELECT_RUL_PART_TYPE, (rs, rowNum) -> rs.getString(CODE));
        for (String code : partCodes) {
            if (!partTypeCodes.contains(code)) {
                partTypeCodes.add(code);
            }
        }
    }

    private void readTypeAndSpecDataFromZipFilePackage(Map<String, ByteArrayInputStream> streamMap) {
        ItemTypes itemTypes = PackageUtils.convertXmlStreamToObject(ItemTypes.class, streamMap.get(ITEM_TYPE_XML));
        if (itemTypes != null) {
            for (ItemType itemType : itemTypes.getItemTypes()) {
                if (!itemTypeMap.keySet().contains(itemType.getCode())) {
                    ItemTypeInfo itemTypeInfo = new ItemTypeInfo(itemType.getCode(), DataType.valueOf(itemType.getDataType()));
                    itemTypeMap.put(itemType.getCode(), itemTypeInfo);
                    itemTypeDataTypeMap.put(itemType.getCode(), DataType.valueOf(itemType.getDataType()));
                }
            }
        }
        ItemSpecs itemSpecs = PackageUtils.convertXmlStreamToObject(ItemSpecs.class, streamMap.get(ITEM_SPEC_XML));
        if (itemSpecs != null) {
            for (ItemSpec itemSpec : itemSpecs.getItemSpecs()) {
                if (!itemSpecCodes.contains(itemSpec.getCode())) {
                    itemSpecCodes.add(itemSpec.getCode());
                    for (ItemTypeAssign itemTypeAssign : itemSpec.getItemTypeAssigns()) {
                        ItemTypeInfo itemTypeInfo = itemTypeMap.get(itemTypeAssign.getCode());
                        List<String> listItemSpecCodes = itemTypeInfo.getSpecs();
                        listItemSpecCodes.add(itemSpec.getCode());
                    }
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
    }

    @Override
    public Collection<String> getPartTypeCodes() {
        return partTypeCodes;
    }

    @Override
    public Collection<String> getItemTypeCodes() {
        return itemTypeMap.keySet();
    }

    @Override
    public Collection<String> getItemSpecCodesByTypeCode(String itemTypeCode) {
        ItemTypeInfo itemType = itemTypeMap.get(itemTypeCode);
        return itemType != null ? itemType.getSpecs() : Collections.emptyList();
    }

    @Override
    public DataType getDataTypeByItemTypeCode(String itemTypeCode) {
        ItemTypeInfo itemType = itemTypeMap.get(itemTypeCode);
        return itemType != null ? itemType.getDataType() : null;
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

    private class ItemTypeInfo {
        final Integer id;
        final String code;
        final DataType dataType;
        List<String> specs = new ArrayList<>();
        public ItemTypeInfo(Integer id, String code, DataType dataType) {
            this.id = id;
            this.code = code;
            this.dataType = dataType;
        }
        public ItemTypeInfo(String code, DataType dataType) {
            this.id = null;
            this.code = code;
            this.dataType = dataType;
        }
        public Integer getId() {
            return id;
        }
        public String getCode() {
            return code;
        }
        public DataType getDataType() {
            return dataType;
        }
        public void setSpecs(List<String> specs) {
            this.specs = specs;
        }
        public List<String> getSpecs() {
            return specs;
        }
    }

    private class RulItemType { // from rul_item_type: id, dataTypeId, code
        final Integer id;
        final Integer dataTypeId;
        final String code;
        RulItemType(Integer id, Integer dataTypeId, String code) {
            this.id = id;
            this.dataTypeId = dataTypeId;
            this.code = code;
        }
        public Integer getId() {
            return id;
        }
        public Integer getDataTypeId() {
            return dataTypeId;
        }
        public String getCode() {
            return code;
        }
    }

    private class RulItemSpec { // from rul_item_spec: id, code
        final Integer id;
        final String code;
        RulItemSpec(Integer id, String code) {
            this.id = id;
            this.code = code;
        }
        public Integer getId() {
            return id;
        }
        public String getCode() {
            return code;
        }
    }

    private class RulItemTypeSpecAssign { // from rul_item_type_spec_assign: typeId, specId
        final Integer typeId;
        final Integer specId;
        RulItemTypeSpecAssign(Integer typeId, Integer specId) {
            this.typeId = typeId;
            this.specId = specId;
        }
        public Integer getTypeId() {
            return typeId;
        }
        public Integer getSpecId() {
            return specId;
        }
    }
}
