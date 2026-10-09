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

/**
 * Assignment of a specification to an item type as one package declares it
 * ({@code <item-type-assign>} of a specification declaration). {@link RulItemTypeSpecAssign} holds the
 * union of these declarations, one row per item type and specification.
 */
@Entity(name = "rul_item_spec_assign_declaration")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RulItemSpecAssignDeclaration {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer itemSpecAssignDeclarationId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpecDeclaration.class)
    @JoinColumn(name = "itemSpecDeclarationId", nullable = false)
    private RulItemSpecDeclaration specDeclaration;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer itemSpecDeclarationId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    private RulItemType itemType;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer itemTypeId;

    /** Order of the assignment among the assignments of the item type in the package's file. */
    @Column(nullable = false)
    private Integer position;

    @Column(length = 50)
    private String viewAfterSpecCode;

    public Integer getItemSpecAssignDeclarationId() {
        return itemSpecAssignDeclarationId;
    }

    public void setItemSpecAssignDeclarationId(final Integer itemSpecAssignDeclarationId) {
        this.itemSpecAssignDeclarationId = itemSpecAssignDeclarationId;
    }

    public RulItemSpecDeclaration getSpecDeclaration() {
        return specDeclaration;
    }

    public Integer getItemSpecDeclarationId() {
        return itemSpecDeclarationId;
    }

    public void setSpecDeclaration(final RulItemSpecDeclaration specDeclaration) {
        this.specDeclaration = specDeclaration;
        this.itemSpecDeclarationId = specDeclaration != null ? specDeclaration.getItemSpecDeclarationId() : null;
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

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    public String getViewAfterSpecCode() {
        return viewAfterSpecCode;
    }

    public void setViewAfterSpecCode(final String viewAfterSpecCode) {
        this.viewAfterSpecCode = viewAfterSpecCode;
    }
}
