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
 * Declaration of a part type by a package ({@code rul_part_type.xml}).
 *
 * <p>Several packages may declare one part type: the part type ({@link RulPartType}) exists once,
 * and each package states its name (in the package's language), child part and repeatable. Name,
 * owner, child part and repeatable of {@link RulPartType} come from the winning declaration (see
 * {@code PackageDeclarations}); declarations of one code should agree.
 */
@Entity(name = "rul_part_type_declaration")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RulPartTypeDeclaration {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer partTypeDeclarationId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPartType.class)
    @JoinColumn(name = "partTypeId", nullable = false)
    private RulPartType partType;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer partTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer packageId;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPartType.class)
    @JoinColumn(name = "childPartId")
    private RulPartType childPart;

    @Column(nullable = false)
    private Boolean repeatable;

    public Integer getPartTypeDeclarationId() {
        return partTypeDeclarationId;
    }

    public void setPartTypeDeclarationId(final Integer partTypeDeclarationId) {
        this.partTypeDeclarationId = partTypeDeclarationId;
    }

    public RulPartType getPartType() {
        return partType;
    }

    public Integer getPartTypeId() {
        return partTypeId;
    }

    public void setPartType(final RulPartType partType) {
        this.partType = partType;
        this.partTypeId = partType != null ? partType.getPartTypeId() : null;
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

    public RulPartType getChildPart() {
        return childPart;
    }

    public void setChildPart(final RulPartType childPart) {
        this.childPart = childPart;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }
}
