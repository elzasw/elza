package cz.tacr.elza.domain;

import org.hibernate.Length;

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
 * Declaration of a specification by a package ({@code rul_item_spec.xml}).
 *
 * <p>Several packages may declare one specification: the specification ({@link RulItemSpec}) exists
 * once. Each declaration states its texts (in the package's language) and category, and the item types
 * it assigns the specification to ({@link RulItemSpecAssignDeclaration}). {@link RulItemSpec} keeps its
 * owner, the package that created it, and the texts in the language of the installation (see
 * {@code PackageDeclarations}); {@link RulItemTypeSpecAssign} holds the union of the assignments.
 */
@Entity(name = "rul_item_spec_declaration")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RulItemSpecDeclaration {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer itemSpecDeclarationId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "itemSpecId", nullable = false)
    private RulItemSpec itemSpec;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer itemSpecId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer packageId;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String shortcut;

    @Column(length = Length.LONG, nullable = false)
    private String description;

    @Column(length = StringLength.LENGTH_1000)
    private String category;

    public Integer getItemSpecDeclarationId() {
        return itemSpecDeclarationId;
    }

    public void setItemSpecDeclarationId(final Integer itemSpecDeclarationId) {
        this.itemSpecDeclarationId = itemSpecDeclarationId;
    }

    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    public Integer getItemSpecId() {
        return itemSpecId;
    }

    public void setItemSpec(final RulItemSpec itemSpec) {
        this.itemSpec = itemSpec;
        this.itemSpecId = itemSpec != null ? itemSpec.getItemSpecId() : null;
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

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(final String shortcut) {
        this.shortcut = shortcut;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }
}
