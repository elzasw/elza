package cz.tacr.elza.packageimport.autoimport;

import java.nio.file.Path;
import java.util.List;

import cz.tacr.elza.packageimport.xml.PackageDependency;
import cz.tacr.elza.packageimport.xml.PackageInfo;

public class PackageInfoWrapper {

    private final PackageInfo pkgInfo;

    private final Path path;

    public PackageInfoWrapper(PackageInfo pkgInfo, Path path) {
        this.pkgInfo = pkgInfo;
        this.path = path;
    }

    public String getCode() {
        return pkgInfo.getCode();
    }

    public Integer getVersion() {
        return pkgInfo.getVersion();
    }

    public List<PackageDependency> getDependencies() {
        return pkgInfo.getDependencies();
    }

    public PackageInfo getPkg() {
        return pkgInfo;
    }

    public Path getPath() {
        return path;
    }

}
