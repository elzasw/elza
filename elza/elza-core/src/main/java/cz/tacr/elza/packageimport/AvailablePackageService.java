package cz.tacr.elza.packageimport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.packageimport.autoimport.AutoImportSelection;
import cz.tacr.elza.packageimport.autoimport.EnabledPackagesConfig;
import cz.tacr.elza.packageimport.autoimport.PackageInfoWrapper;
import cz.tacr.elza.packageimport.xml.PackageDependency;
import cz.tacr.elza.repository.PackageRepository;

/**
 * Packages of the {@code dpkg} directory that are not installed, and their mark for the import at
 * the next start.
 *
 * A mark is a {@link RulPackage} row with {@link RulPackage#PENDING_VERSION} and the description
 * of the package from its file; the startup import treats it as installed and imports the file.
 * Cancelling the mark deletes the row. The directory is read on every request, so a file copied
 * while the application runs is listed.
 */
@Service
public class AvailablePackageService {

    private static final Logger logger = LoggerFactory.getLogger(AvailablePackageService.class);

    /** What the next start does with a package of the directory that is not installed. */
    public enum State {
        /** Listed in the configuration, or required by a listed or marked package. */
        CONFIGURED,
        /** Marked in the administration. */
        MARKED,
        /** Left alone. */
        NOT_LOADED
    }

    /** A package of the directory that is not installed. */
    public record AvailablePackage(PackageInfoWrapper pkg, State state) {
    }

    @Autowired
    private PackageRepository packageRepository;

    @Autowired
    private PackageService packageService;

    @Autowired
    private PackageTranslationService packageTranslationService;

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private Environment environment;

    @Transactional(readOnly = true)
    public List<AvailablePackage> listAvailable() {
        return listAvailable(resourcePathResolver.getDpkgDir());
    }

    /**
     * Packages of the directory that are not installed, by code. A marked package is listed; an
     * installed one is not, whatever the version of its file.
     */
    @Transactional(readOnly = true)
    public List<AvailablePackage> listAvailable(final Path dpkgDir) {
        Map<String, PackageInfoWrapper> available = readDirectory(dpkgDir);
        Map<String, RulPackage> installed = packageRepository.findAll().stream()
                .collect(Collectors.toMap(RulPackage::getCode, p -> p));
        Set<String> installedCodes = installed.keySet();
        AutoImportSelection.Result selection = AutoImportSelection.select(EnabledPackagesConfig.read(environment),
                                                                         installedCodes, available);
        List<AvailablePackage> result = new ArrayList<>();
        available.values().stream()
                .sorted((a, b) -> a.getCode().compareTo(b.getCode()))
                .forEach(pkg -> {
                    RulPackage row = installed.get(pkg.getCode());
                    if (row != null && !row.isPending()) {
                        return;
                    }
                    State state = row != null ? State.MARKED
                            : selection.codes().contains(pkg.getCode()) ? State.CONFIGURED : State.NOT_LOADED;
                    result.add(new AvailablePackage(pkg, state));
                });
        return result;
    }

    @Transactional
    public void mark(final String code) {
        mark(resourcePathResolver.getDpkgDir(), code);
    }

    /**
     * Marks a package of the directory for the import at the next start. Refused when the package
     * is installed, or when a package it requires is neither installed, marked nor in the directory.
     * Marking a marked package does nothing.
     */
    @Transactional
    public void mark(final Path dpkgDir, final String code) {
        Map<String, PackageInfoWrapper> available = readDirectory(dpkgDir);
        PackageInfoWrapper pkg = available.get(code);
        if (pkg == null) {
            throw new ObjectNotFoundException("Balíček " + code + " není v adresáři " + dpkgDir,
                    PackageCode.PACKAGE_NOT_EXIST).set("code", code);
        }
        RulPackage row = packageRepository.findByCode(code);
        if (row != null) {
            if (row.isPending()) {
                return;
            }
            throw new BusinessException("Balíček " + code + " je již naimportován", PackageCode.ALREADY_INSTALLED)
                    .set("code", code);
        }
        List<String> missing = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(pkg.getDependencies())) {
            for (PackageDependency dependency : pkg.getDependencies()) {
                String required = dependency.getCode();
                if (!available.containsKey(required) && packageRepository.findByCode(required) == null) {
                    missing.add(required);
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new BusinessException("Balíčky nenalezeny: " + missing, PackageCode.FOREIGN_PACKAGES_NOT_EXIST)
                    .set("codes", missing);
        }

        RulPackage pending = new RulPackage();
        pending.setCode(pkg.getCode());
        pending.setName(pkg.getPkg().getName());
        pending.setDescription(StringUtils.defaultString(pkg.getPkg().getDescription()));
        pending.setVersion(RulPackage.PENDING_VERSION);
        pending.setLanguage(packageTranslationService.resolvePackageLanguage(pkg.getPkg().getLanguage()));
        packageRepository.save(pending);
        staticDataService.reloadOnCommit();
        logger.info("Package {} marked for the import at the next start, file {}", code, pkg.getPath());
    }

    /**
     * Cancels the mark of a package: deletes the row. Refused when the package is installed.
     */
    public void unmark(final String code) {
        RulPackage row = packageRepository.findByCode(code);
        if (row == null) {
            throw new ObjectNotFoundException("Balíček " + code + " není označen", PackageCode.PACKAGE_NOT_EXIST)
                    .set("code", code);
        }
        if (!row.isPending()) {
            throw new BusinessException("Balíček " + code + " je naimportován, značku nelze zrušit",
                    PackageCode.ALREADY_INSTALLED).set("code", code);
        }
        packageService.deletePackage(code);
        logger.info("Mark of package {} cancelled", code);
    }

    /**
     * A restart is needed: a package is marked for the next start, or a package was imported while
     * the application runs (the search index registers its new fields after a restart).
     */
    @Transactional(readOnly = true)
    public boolean isRestartRequired() {
        return packageService.isImportedSinceStart()
                || packageRepository.findAll().stream().anyMatch(RulPackage::isPending);
    }

    private static Map<String, PackageInfoWrapper> readDirectory(final Path dpkgDir) {
        try {
            return PackageUtils.readPackageDirectory(dpkgDir);
        } catch (IOException e) {
            throw new SystemException("Error reading the package directory " + dpkgDir, e);
        }
    }
}
