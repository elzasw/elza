package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


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
    private Integer packageDependencyId;

    /**
     * Balíček který vyžaduje závislost na jiný.
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "sourcePackageId", nullable = false)
    private RulPackage sourcePackage;

    @Column(name = "sourcePackageId", updatable = false, insertable = false)
    private Integer sourcePackageId;

    /**
     * Balíček na který je tvořena závislost.
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "targetPackageId", nullable = false)
    private RulPackage targetPackage;

    @Column(name = "targetPackageId", updatable = false, insertable = false)
    private Integer targetPackageId;

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
    public RulPackage getSourcePackage() {
        return sourcePackage;
    }

    /**
     * @param sourcePackage balíček na který je tvořena závislost
     */
    public void setSourcePackage(final RulPackage sourcePackage) {
        this.sourcePackage = sourcePackage;
        this.sourcePackageId = sourcePackage == null ? null : sourcePackage.getPackageId();
    }

    /**
     * @return identifikátor balíčeku na který je tvořena závislost
     */
    public Integer getSourcePackageId() {
        return sourcePackageId;
    }

    /**
     * @return balíček na který je tvořena závislost
     */
    public RulPackage getTargetPackage() {
        return targetPackage;
    }

    /**
     * @param targetPackage balíček na který je tvořena závislost
     */
    public void setTargetPackage(final RulPackage targetPackage) {
        this.targetPackage = targetPackage;
        this.targetPackageId = targetPackage == null ? null : targetPackage.getPackageId();
    }

    /**
     * @return identifikátor balíčku na který je tvořena závislost
     */
    public Integer getTargetPackageId() {
        return targetPackageId;
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
