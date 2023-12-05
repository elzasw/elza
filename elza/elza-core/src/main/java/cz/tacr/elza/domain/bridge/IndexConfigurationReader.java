package cz.tacr.elza.domain.bridge;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.packageimport.PackageContext;
import cz.tacr.elza.packageimport.PackageUtils;
import cz.tacr.elza.packageimport.autoimport.PackageInfoWrapper;
import cz.tacr.elza.packageimport.xml.ItemType;
import cz.tacr.elza.packageimport.xml.ItemTypes;
import cz.tacr.elza.packageimport.xml.PackageInfo;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static cz.tacr.elza.packageimport.PackageService.ITEM_TYPE_XML;

/**
 * Místo pro načtení konfigurace Lucene indexu, před vlastní inicializací indexu.
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 */
@Component
public class IndexConfigurationReader {

    //Toto NEFUNGUJE!! závislost na bean co je závislý na Hibernate
    //@Autowired
    //AeBatchRepository aeBatchRepository;

    //Toto je OK funguje
    @Autowired
    JdbcTemplate jdbcTemplate;

    private static final String PACKAGE_XML = "package.xml";

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Value("${elza.package.testing:false}")
    private Boolean testing;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<PackageInfoWrapper> packagesToImport;

    private List<PackageInfoWrapper> allPackages;
    private List<String> importedItemTypeCodes;

    Map<String, PackageInfoWrapper> latestVersionMap;


    @PostConstruct
    public void init() {
        packagesToImport = new ArrayList<>();
        importedItemTypeCodes = new ArrayList<>();
        Path dpkgDir = resourcePathResolver.getDpkgDir();
        if (!Files.exists(dpkgDir)) {
            return;
        }

        logger.info("Checking folder {} for packages...", dpkgDir);

        // get current packages from DB
        /*List<RulPackage> packagesDb = getPackages(); //TODO načíst z DB

        Map<String, PackageInfoWrapper> latestVersionMap = packagesDb.stream().map(p -> getPackageInfo(p))
                .collect(Collectors.toMap(PackageInfo::getCode, p -> new PackageInfoWrapper(p, null)));*/

        String packageSql = "SELECT * FROM rul_package";
        List<PackageInfo> packageInfoList = jdbcTemplate.query(packageSql, (rs, rowNum) -> {

            PackageInfo packageInfo = new PackageInfo();

            packageInfo.setCode(rs.getString("code"));
            packageInfo.setName(rs.getString("name"));
            packageInfo.setDescription(rs.getString("description"));
            packageInfo.setVersion(rs.getInt("version"));

            return packageInfo;
        });
        String itemTypeSql = "SELECT * FROM rul_item_type";
        List<String> codes = jdbcTemplate.query(itemTypeSql, (rs, rowNum) -> rs.getString("code"));
        importedItemTypeCodes.addAll(codes);

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
                if (mapPkg == null ||
                        mapPkg.getVersion() < pkg.getVersion() ||
                        (testing && mapPkg.getVersion() <= pkg.getVersion())) {
                    packagesToImport.add(new PackageInfoWrapper(pkg.getPkg(), path));
                    latestVersionMap.put(pkg.getCode(), new PackageInfoWrapper(pkg.getPkg(), path));
                    PackageContext pkgCtx = new PackageContext(resourcePathResolver);
                    pkgCtx.init(pkg.getPath().toFile());
                    ItemTypes itemTypes = pkgCtx.convertXmlStreamToObject(ItemTypes.class, ITEM_TYPE_XML);
                    for (ItemType itemType : itemTypes.getItemTypes()) {
                        if (!importedItemTypeCodes.contains(itemType.getCode())) {
                            importedItemTypeCodes.add(itemType.getCode());
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

        //jdbcTemplate.
    }

    public List<String> getImportedItemTypeCodes() {
        return importedItemTypeCodes;
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
