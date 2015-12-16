package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Implementace RulPackageActions.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@Entity(name = "rul_package_actions")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulPackageActions implements cz.tacr.elza.api.RulPackageActions<RulPackage> {

    @Id
    @GeneratedValue
    private Integer packageActionsId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(length = 250, nullable = false)
    private String filename;

    @Override
    public Integer getPackageActionsId() {
        return packageActionsId;
    }

    @Override
    public void setPackageActionsId(final Integer packageActionsId) {
        this.packageActionsId = packageActionsId;
    }

    @Override
    public RulPackage getPackage() {
        return rulPackage;
    }

    @Override
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(final String filename) {
        this.filename = filename;
    }
}
