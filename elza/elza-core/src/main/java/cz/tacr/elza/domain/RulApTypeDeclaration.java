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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Declaration of an entity class by a package ({@code ap_type.xml}).
 *
 * <p>Several packages may declare one class: the class ({@link ApType}) exists once, with one code
 * and id, and each package states its name (in the package's language), parent and read-only. The
 * parents of all declarations must agree. The name, owner and read-only of {@link ApType} summarize
 * the declarations (see {@code ApTypeDeclarations}); the names of all declarations are offered as
 * texts of the class in the languages of the packages.
 */
@Entity(name = "rul_ap_type_declaration")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RulApTypeDeclaration {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer apTypeDeclarationId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = "apTypeId", nullable = false)
    private ApType apType;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer apTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer packageId;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = "parentApTypeId")
    private ApType parentApType;

    @Column(insertable = false, updatable = false)
    private Integer parentApTypeId;

    @Column(nullable = false)
    private Boolean readOnly;

    public Integer getApTypeDeclarationId() {
        return apTypeDeclarationId;
    }

    public void setApTypeDeclarationId(final Integer apTypeDeclarationId) {
        this.apTypeDeclarationId = apTypeDeclarationId;
    }

    public ApType getApType() {
        return apType;
    }

    public Integer getApTypeId() {
        return apTypeId;
    }

    public void setApType(final ApType apType) {
        this.apType = apType;
        this.apTypeId = apType != null ? apType.getApTypeId() : null;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
        this.packageId = rulPackage != null ? rulPackage.getPackageId() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public ApType getParentApType() {
        return parentApType;
    }

    public Integer getParentApTypeId() {
        return parentApTypeId;
    }

    public void setParentApType(final ApType parentApType) {
        this.parentApType = parentApType;
        this.parentApTypeId = parentApType != null ? parentApType.getApTypeId() : null;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final Boolean readOnly) {
        this.readOnly = readOnly;
    }
}
