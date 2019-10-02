package cz.tacr.elza.controller.vo;

import java.util.List;

/**
 * VO pro balíček.
 *
 * @since 15.09.2017
 */
public class PackageVO {

    /**
     * ID.
     */
    private Integer packageId;

    /**
     * Název balíčku.
     */
    private String name;

    /**
     * Kód balíčku.
     */
    private String code;

    /**
     * Popis balíčku.
     */
    private String description;

    /**
     * Verze balíčku.
     */
    private Integer version;

    /**
     * Závislé balíčky.
     */
    private List<PackageDependencyVO> dependencies;

    /**
     * Závislosti z balíčků.
     */
    private List<PackageDependencyVO> dependenciesBy;

    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(final Integer packageId) {
        this.packageId = packageId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public List<PackageDependencyVO> getDependencies() {
        return dependencies;
    }

    public void setDependencies(final List<PackageDependencyVO> dependencies) {
        this.dependencies = dependencies;
    }

    public List<PackageDependencyVO> getDependenciesBy() {
        return dependenciesBy;
    }

    public void setDependenciesBy(final List<PackageDependencyVO> dependenciesBy) {
        this.dependenciesBy = dependenciesBy;
    }
}
