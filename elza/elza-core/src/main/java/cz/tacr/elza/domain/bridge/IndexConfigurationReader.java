package cz.tacr.elza.domain.bridge;

import static cz.tacr.elza.packageimport.PackageService.ITEM_TYPE_XML;
import static cz.tacr.elza.packageimport.PackageService.PART_TYPE_XML;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import cz.tacr.elza.packageimport.xml.ItemType;
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

    private Map<String, PackageInfoWrapper> latestVersionMap;

    @Value("${elza.workingDir}")
    private String workDir;

    @PostConstruct
    public void init() {
        packagesToImport = new ArrayList<>();
        partTypeCodes = new ArrayList<>();
        itemTypeCodes = new ArrayList<>();

        // get item type codes from DB
        List<String> itemCodes = jdbcTemplate.query("SELECT * FROM rul_item_type", (rs, rowNum) -> rs.getString("code"));
        itemTypeCodes.addAll(itemCodes);

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

                    PartTypes partTypes = PackageUtils.convertXmlStreamToObject(PartTypes.class, streamMap.get(PART_TYPE_XML));
                    for (PartType partType : partTypes.getPartTypes()) {
                        if (!partTypeCodes.contains(partType.getCode())) {
                            partTypeCodes.add(partType.getCode());
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
}
