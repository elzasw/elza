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
 * Declaration of an item type by a package ({@code rul_item_type.xml}).
 *
 * <p>Several packages may declare one item type: the item type ({@link RulItemType}) exists once.
 * The values deciding how data are stored - data type, use of specifications, structured type,
 * columns of a table - must agree and are kept only in {@link RulItemType}. Each declaration states
 * its texts (in the package's language) and the values below; {@link RulItemType} takes them from
 * the declaration of its owner, the package that created it, and its texts in the language of the
 * installation (see {@code PackageDeclarations}).
 */
@Entity(name = "rul_item_type_declaration")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RulItemTypeDeclaration {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer itemTypeDeclarationId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    private RulItemType itemType;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer itemTypeId;

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

    @Column(nullable = false)
    private Boolean canBeOrdered;

    @Column
    private Integer stringLengthLimit;

    /** JSON as in {@link RulItemType} (columns, display type or mask). */
    @Column
    private String viewDefinition;

    public Integer getItemTypeDeclarationId() {
        return itemTypeDeclarationId;
    }

    public void setItemTypeDeclarationId(final Integer itemTypeDeclarationId) {
        this.itemTypeDeclarationId = itemTypeDeclarationId;
    }

    public RulItemType getItemType() {
        return itemType;
    }

    public Integer getItemTypeId() {
        return itemTypeId;
    }

    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;
        this.itemTypeId = itemType != null ? itemType.getItemTypeId() : null;
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

    public Boolean getCanBeOrdered() {
        return canBeOrdered;
    }

    public void setCanBeOrdered(final Boolean canBeOrdered) {
        this.canBeOrdered = canBeOrdered;
    }

    public Integer getStringLengthLimit() {
        return stringLengthLimit;
    }

    public void setStringLengthLimit(final Integer stringLengthLimit) {
        this.stringLengthLimit = stringLengthLimit;
    }

    public String getViewDefinition() {
        return viewDefinition;
    }

    public void setViewDefinition(final String viewDefinition) {
        this.viewDefinition = viewDefinition;
    }
}
