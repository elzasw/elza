package cz.tacr.elza.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tacr.elza.controller.vo.AvailablePackage;
import cz.tacr.elza.controller.vo.AvailablePackageDependency;
import cz.tacr.elza.controller.vo.AvailablePackageState;
import cz.tacr.elza.controller.vo.AvailablePackages;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.packageimport.AvailablePackageService;
import cz.tacr.elza.packageimport.autoimport.PackageInfoWrapper;
import cz.tacr.elza.service.RestartService;

/**
 * Packages of the {@code dpkg} directory that are not installed, and their mark for the import at
 * the next start (tag {@code packages}); administrators only.
 */
@RestController
@RequestMapping("/api/v1")
public class PackagesController implements PackagesApi {

    @Autowired
    private AvailablePackageService availablePackageService;

    @Autowired
    private RestartService restartService;

    @Override
    @Transactional(readOnly = true)
    @AuthMethod(permission = {Permission.ADMIN})
    public ResponseEntity<AvailablePackages> packagesListAvailablePackages() {
        List<AvailablePackage> items = availablePackageService.listAvailable().stream()
                .map(PackagesController::toVO)
                .collect(Collectors.toList());
        AvailablePackages result = new AvailablePackages();
        result.setItems(items);
        result.setRestartRequired(availablePackageService.isRestartRequired());
        result.setRestartAvailable(restartService.isAvailable());
        return ResponseEntity.ok(result);
    }

    @Override
    @Transactional
    @AuthMethod(permission = {Permission.ADMIN})
    public ResponseEntity<Void> packagesMarkPackage(final String code) {
        availablePackageService.mark(code);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @AuthMethod(permission = {Permission.ADMIN})
    public ResponseEntity<Void> packagesUnmarkPackage(final String code) {
        availablePackageService.unmark(code);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private static AvailablePackage toVO(final AvailablePackageService.AvailablePackage item) {
        PackageInfoWrapper pkg = item.pkg();
        AvailablePackage vo = new AvailablePackage();
        vo.setCode(pkg.getCode());
        vo.setName(pkg.getPkg().getName());
        vo.setVersion(pkg.getVersion());
        vo.setDescription(pkg.getPkg().getDescription());
        vo.setDependencies(pkg.getDependencies() == null ? List.of()
                : pkg.getDependencies().stream().map(d -> {
                    AvailablePackageDependency dependency = new AvailablePackageDependency();
                    dependency.setCode(d.getCode());
                    dependency.setMinVersion(d.getMinVersion());
                    return dependency;
                }).collect(Collectors.toList()));
        vo.setState(AvailablePackageState.valueOf(item.state().name()));
        vo.setFileName(pkg.getPath().getFileName().toString());
        return vo;
    }
}
