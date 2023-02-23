package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


/**
 * Vazební entita pro závislosti balíčku.
 *
 * @since 14.09.2017
 */
@Entity(name = "rul_package_dependency")
@Table
public class RulPackageDependency {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer packageDependencyId;

    /**
     * Balíček který vyžaduje závislost na jiný.
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(name = "packageId", updatable = false, insertable = false)
    private Integer packageId;

    /**
     * Balíček na který je tvořena závislost.
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "dependsOnPackageId", nullable = false)
    private RulPackage dependsOnPackage;

    @Column(name = "dependsOnPackageId", updatable = false, insertable = false)
    private Integer dependsOnPackageId;

    /**
     * Minimální verze, kterou cílový balíček musí mít.
     */
    @Column(nullable = false)
    private Integer minVersion;

    /**
     * @return identifikátor entity
     */
    public Integer getPackageDependencyId() {
        return packageDependencyId;
    }

    /**
     * @param packageDependencyId nastavení identifikátoru entity
     */
    public void setPackageDependencyId(final Integer packageDependencyId) {
        this.packageDependencyId = packageDependencyId;
    }

    /**
     * @return balíček na který je tvořena závislost
     */
    public RulPackage getRulPackage() {
        return rulPackage;
    }

    /**
     * @param rulPackage balíček na který je tvořena závislost
     */
    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
        this.packageId = rulPackage == null ? null : rulPackage.getPackageId();
    }

    /**
     * @return identifikátor balíčeku na který je tvořena závislost
     */
    public Integer getPackageId() {
        return packageId;
    }

    /**
     * @return balíček na který je tvořena závislost
     */
    public RulPackage getDependsOnPackage() {
        return dependsOnPackage;
    }

    /**
     * @param dependsOnPackage balíček na který je tvořena závislost
     */
    public void setDependsOnPackage(final RulPackage dependsOnPackage) {
        this.dependsOnPackage = dependsOnPackage;
        this.dependsOnPackageId = dependsOnPackage == null ? null : dependsOnPackage.getPackageId();
    }

    /**
     * @return identifikátor balíčku na který je tvořena závislost
     */
    public Integer getDependsOnPackageId() {
        return dependsOnPackageId;
    }

    /**
     * @return minimální požadovaná verze balíčku
     */
    public Integer getMinVersion() {
        return minVersion;
    }

    /**
     * @param minVersion minimální požadovaná verze balíčku
     */
    public void setMinVersion(final Integer minVersion) {
        this.minVersion = minVersion;
    }
}
